package io.feedback

object MaxHealthLoad {
  val Init = MaxHealthLoad(50, 0)
}
case class MaxHealthLoad(v: Double, step: Int)

trait AdmissionController {
  val name: String

  /**
    *
    * @param step
    * @param allowedLoad
    * @param maxLoad
    * @return (load, throttling, history maximum healthy load)
    */
  def apply(step: Int, allowedLoad: Double, maxLoad: MaxHealthLoad): (Double, Double, MaxHealthLoad)
}

/**
  * An controller that does not change the plant's input and throttling.
  */
class IdentityController(plant: Server) extends AdmissionController {
  override val name = "identity"

  def apply(step: Int, input: Double, maxLoad: MaxHealthLoad): (Double, Double, MaxHealthLoad) = {
    val load = plant.load(step)
    val throttled = plant.throttle(step, plant.load(step))
    println(s"\nstep, $step, input: $input, maxLoad: $maxLoad, throttled: $throttled")
    (load, throttled, maxLoad)
  }

}

/**
  * Given the current load and throttling number, adjust permitted load
  *
  * @param plant
  */
class StandardController(plant: Server) extends AdmissionController {
  import Server._

  override val name = "standard"

  private[this] val window: Int = 5
  private[this] val assignedCpuNs = lowWatermark * window
  private[this] val maxCpuNs = highWatermark * window

  private[this] var consecutiveHealthyWins = 0
  private[this] var consecutiveThrottlingWins = 0

  private[this] val accumulatedThrottleNs = new WindowedUtil(window)

  private[this] var isHealthy = true

  private[this] var prevThrottled = 0.0

  /**
    * The controller does the following:
    * 1. tracks the maximum load under healthy state
    * 2. maintains allowed load based on throttling states
    *
    * Health is defined as: 5 consecutive steps without throttling.
    *
    * When it sees two consecutive throttling, it sets `allowedLoad` to be `maxHealthyLoad`,
    * and the controller enters the rate limit mode.
    * If still gets throttled with rate limit, decrease `allowedLoad` according to a window
    * of history throttling time.
    * If it's not throttled in the current step, but does not yet have 5 consecutive non-throttling
    * steps, do nothing.
    * If there is 5 consecutive non-throttling steps, increase `allowedLoad` slowly.
    *
    * In healthy steps, `maxHealthyLoad` is updated.
    *
    * @param step
    * @param allowedLoad
    * @param maxHealthyLoad
    * @return (next allowed load, next throttling, maximum healthy load)
    */
  def apply(step: Int, allowedLoad: Double, maxHealthyLoad: MaxHealthLoad): (Double, Double, MaxHealthLoad) = {
    val externalLoad = plant.load(step)
    val processedLoad = math.min(allowedLoad, externalLoad)

    // tracks whether throttled in the previous step
    val isPrevHealthy = prevThrottled <= 0
    val throttled = plant.throttle(step, processedLoad)
    prevThrottled = throttled

    // a window of history throttling
    accumulatedThrottleNs.add(throttled)

    println(s"\nstep: $step, load: $externalLoad, rate limit: $allowedLoad, throttled: $throttled, max health load: $maxHealthyLoad")

    val nextAllow =
      if (throttled == 0) {
        if (isPrevHealthy) consecutiveHealthyWins += 1
        else consecutiveHealthyWins = 1

        // 5 consecutive healthy
        if (consecutiveHealthyWins >= window) {
          isHealthy = true

          // is rate limited, bump up limit
          if (processedLoad < externalLoad) {
            if (processedLoad < maxHealthyLoad.v) {
              val increase = (maxHealthyLoad.v - processedLoad) / 2
              println(s"increase: $increase")
              processedLoad + increase
            } else {
              println(s"increase by 1%")
              processedLoad * 1.01
            }
          } else allowedLoad
        } else allowedLoad
      } else {
        if (isPrevHealthy) consecutiveThrottlingWins = 1
        else consecutiveThrottlingWins += 1

        if (isHealthy && consecutiveThrottlingWins == 1) {
          // only one throttling, no-op
          allowedLoad
        } else if (consecutiveHealthyWins >= 5 && consecutiveThrottlingWins == 2) {
          isHealthy = false
          // allow rate drop to history max
          maxHealthyLoad.v
        } else {
          val throttledSum = accumulatedThrottleNs.getSum

          if (throttledSum < assignedCpuNs) {
            println(s"decrease proportion: ${throttledSum / assignedCpuNs.toDouble}")
            allowedLoad * (1 - throttledSum / assignedCpuNs.toDouble)
          } else if (throttledSum < maxCpuNs) {
            println(s"decrease 1/4")
            allowedLoad * 0.75
          } else {
            // impossible
            1.0
          }
        }
      }

    // update max health load
    val nextMaxLoad =
      if (throttled == 0) {
        // if current load > history max, or history max is too old
        if (processedLoad > maxHealthyLoad.v || step - maxHealthyLoad.step >= 10) {
          println(s"update max to: $processedLoad")
          MaxHealthLoad(processedLoad, step)
        } else maxHealthyLoad
      } else maxHealthyLoad

    println(s"health wins: $consecutiveHealthyWins, throttled wins: $consecutiveThrottlingWins")

    (nextAllow, throttled, nextMaxLoad)
  }
}
