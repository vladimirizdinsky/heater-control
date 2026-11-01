package io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model

import io.github.vladimirizdinsky.heatercontrol.core.network.ApiEnum

// Known ids and values of the heater-hub vocabulary (`docs/api.md`, section 7).
// The mapper drops unknown ones (section 8), so a new server value never breaks the app.

/** Section 7.2. */
enum class AttributeId(override val apiId: String) : ApiEnum {
    POWER("power"),
    TARGET_TEMPERATURE("targetTemperature"),
    CURRENT_TEMPERATURE("currentTemperature"),
    HEATING("heating"),
    MODE("mode"),
    ECO_MODE("ecoMode"),
    CHILD_LOCK("childLock"),
    DISPLAY_LIGHT("displayLight"),
    FAN_SPEED("fanSpeed"),
    FAN_RUNNING("fanRunning"),
    OSCILLATION("oscillation"),
    OSCILLATION_ANGLE("oscillationAngle"),
    NIGHT_MODE("nightMode"),
    SLEEP_TIMER("sleepTimer"),
}

/** Section 7.4: one set function per writable attribute. */
enum class FunctionId(override val apiId: String) : ApiEnum {
    SET_POWER("setPower"),
    SET_TARGET_TEMPERATURE("setTargetTemperature"),
    SET_MODE("setMode"),
    SET_ECO_MODE("setEcoMode"),
    SET_CHILD_LOCK("setChildLock"),
    SET_DISPLAY_LIGHT("setDisplayLight"),
    SET_FAN_SPEED("setFanSpeed"),
    SET_OSCILLATION("setOscillation"),
    SET_OSCILLATION_ANGLE("setOscillationAngle"),
    SET_NIGHT_MODE("setNightMode"),
    SET_SLEEP_TIMER("setSleepTimer"),
}

/**
 * Section 7.3. [heats] is the client's fixed split used by the heating chip (spec 5.1):
 * only `fan` and `noHeat` do not heat.
 */
enum class ModeValue(override val apiId: String, val heats: Boolean) : ApiEnum {
    HIGH("high", heats = true),
    LOW("low", heats = true),
    NO_HEAT("noHeat", heats = false),
    COMFORT("comfort", heats = true),
    ECO("eco", heats = true),
    ANTI_FREEZE("antiFreeze", heats = true),
    MANUAL("manual", heats = true),
    HOLIDAY("holiday", heats = true),
    SCHEDULE("schedule", heats = true),
    HEAT("heat", heats = true),
    FAN("fan", heats = false),
}

/** Unit of a number attribute (section 4.2). Not named `Unit` to avoid `kotlin.Unit`. */
enum class MeasureUnit(override val apiId: String) : ApiEnum {
    CELSIUS("celsius"),
    MINUTES("minutes"),
    DEGREES("degrees"),
}

/** Section 7.5. An unknown code is still shown, as a generic fault. */
enum class FaultCode(override val apiId: String) : ApiEnum {
    TILT("tilt"),
    DEVICE_FAULT("deviceFault"),
}

/** Section 7.6. */
enum class UnavailableReason(override val apiId: String) : ApiEnum {
    OFFLINE("offline"),
    POWER_OFF("powerOff"),
}

/** Section 7.7. */
enum class CommandStatus(override val apiId: String) : ApiEnum {
    PENDING("pending"),
    CONFIRMED("confirmed"),
    FAILED("failed"),
    ABORTED("aborted"),
}

/** Section 7.7. */
enum class CommandReason(override val apiId: String) : ApiEnum {
    NOT_CONFIRMED("notConfirmed"),
    OVERRIDDEN("overridden"),
    SUPERSEDED("superseded"),
}
