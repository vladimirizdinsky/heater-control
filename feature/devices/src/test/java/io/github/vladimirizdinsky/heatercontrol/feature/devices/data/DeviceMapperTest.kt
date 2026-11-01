package io.github.vladimirizdinsky.heatercontrol.feature.devices.data

import io.github.vladimirizdinsky.heatercontrol.core.network.HttpClientFactory
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.CommandDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.data.dto.DeviceDto
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.AttributeId
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.AttributeValue
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.CommandReason
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.CommandStatus
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.Device
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.FaultCode
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.FunctionId
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.Input
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.MeasureUnit
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.ModeValue
import io.github.vladimirizdinsky.heatercontrol.feature.devices.domain.model.UnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

private const val BASE_URL = "http://192.168.0.39:8080"

/** JSON shaped like the examples in heater-hub `docs/api.md`, sections 4 and 7. */
class DeviceMapperTest {

    @Test
    fun `maps a Dyson with attributes and functions in server order`() {
        val device = map(DYSON)

        assertEquals("XXX-EU-XXX0000A", device.id)
        assertEquals("Obývačka", device.displayName)
        assertEquals("Dyson Hot+Cool HF1", device.model)
        assertTrue(device.online)
        assertEquals(Instant.parse("2026-10-26T18:04:11Z"), device.updatedAt)
        assertEquals(
            listOf(
                AttributeId.POWER, AttributeId.TARGET_TEMPERATURE, AttributeId.CURRENT_TEMPERATURE,
                AttributeId.HEATING, AttributeId.MODE, AttributeId.FAN_SPEED, AttributeId.SLEEP_TIMER,
            ),
            device.attributes.keys.toList(),
        )
        assertEquals(
            listOf(FunctionId.SET_POWER, FunctionId.SET_TARGET_TEMPERATURE, FunctionId.SET_MODE, FunctionId.SET_SLEEP_TIMER),
            device.functions.keys.toList(),
        )
    }

    @Test
    fun `maps attribute values by type`() {
        val attributes = map(DYSON).attributes

        assertEquals(AttributeValue.Bool(true), attributes[AttributeId.POWER])
        assertEquals(AttributeValue.Number(21.0, MeasureUnit.CELSIUS), attributes[AttributeId.TARGET_TEMPERATURE])
        assertEquals(AttributeValue.Number(4.0, null), attributes[AttributeId.FAN_SPEED])
        assertEquals(
            AttributeValue.Enum(ModeValue.HEAT, listOf(ModeValue.HEAT, ModeValue.FAN)),
            attributes[AttributeId.MODE],
        )
    }

    @Test
    fun `null value means unknown or off`() {
        val attributes = map(DYSON).attributes

        assertEquals(AttributeValue.Number(null, MeasureUnit.CELSIUS), attributes[AttributeId.CURRENT_TEMPERATURE])
        assertEquals(AttributeValue.Number(null, MeasureUnit.MINUTES), attributes[AttributeId.SLEEP_TIMER])
    }

    @Test
    fun `maps functions with inputs and availability`() {
        val functions = map(DYSON).functions

        val setTemperature = functions.getValue(FunctionId.SET_TARGET_TEMPERATURE)
        assertEquals(AttributeId.TARGET_TEMPERATURE, setTemperature.attribute)
        assertEquals(Input.Number(min = 12.0, max = 32.0, step = 1.0, nullable = false), setTemperature.input)
        assertFalse(setTemperature.available)
        assertEquals(UnavailableReason.POWER_OFF, setTemperature.unavailableReason)

        assertEquals(Input.Bool(nullable = false), functions.getValue(FunctionId.SET_POWER).input)
        assertEquals(
            Input.Enum(listOf(ModeValue.HEAT, ModeValue.FAN), nullable = false),
            functions.getValue(FunctionId.SET_MODE).input,
        )
        assertEquals(
            Input.Number(min = 0.0, max = 540.0, step = 15.0, nullable = true),
            functions.getValue(FunctionId.SET_SLEEP_TIMER).input,
        )
    }

    @Test
    fun `drops unknown attributes, functions, options and input types`() {
        val device = map(UNKNOWN_VALUES)

        // humidity and an attribute with an unknown type
        assertEquals(listOf(AttributeId.POWER, AttributeId.MODE), device.attributes.keys.toList())
        // setHumidity, and boost because of its unknown input type; a function without input stays
        assertEquals(listOf(FunctionId.SET_POWER, FunctionId.SET_MODE), device.functions.keys.toList())
        assertNull(device.functions.getValue(FunctionId.SET_POWER).input)
        assertEquals(
            Input.Enum(listOf(ModeValue.COMFORT, ModeValue.ECO), nullable = false),
            device.functions.getValue(FunctionId.SET_MODE).input,
        )
    }

    @Test
    fun `unknown current mode becomes null but known options stay`() {
        val mode = map(UNKNOWN_VALUES).attributes[AttributeId.MODE]

        assertEquals(AttributeValue.Enum(null, listOf(ModeValue.COMFORT, ModeValue.ECO)), mode)
    }

    @Test
    fun `faults are never dropped, unknown ones keep their raw code`() {
        val faults = map(UNKNOWN_VALUES).faults.orEmpty()

        assertEquals(3, faults.size)
        assertEquals(FaultCode.TILT, faults[0].code)
        assertEquals(FaultCode.DEVICE_FAULT, faults[1].code)
        assertEquals("dyson:fs03", faults[1].detail)
        assertNull(faults[2].code)
        assertEquals("overheated", faults[2].rawCode)
    }

    @Test
    fun `faults distinguish none from not known yet`() {
        assertEquals(emptyList<Any>(), map(DYSON).faults)
        assertNull(map(UNKNOWN_VALUES.replace(FAULTS_JSON, "null")).faults)
    }

    @Test
    fun `image is resolved against the base URL, also one with a path`() {
        assertEquals("$BASE_URL/images/dyson-hot-cool-hf1.webp", map(DYSON).imageUrl)
        assertEquals(
            "https://pi.tailnet.ts.net/hub/images/dyson-hot-cool-hf1.webp",
            map(DYSON, baseUrl = "https://pi.tailnet.ts.net/hub/").imageUrl,
        )
        assertNull(map(UNKNOWN_VALUES).imageUrl)
    }

    @Test
    fun `display name falls back to the model`() {
        val device = map(UNKNOWN_VALUES)

        assertNull(device.friendlyName)
        assertEquals("Klarstein Bansin", device.displayName)
    }

    @Test
    fun `offline device with a pending command and a malformed timestamp`() {
        val device = map(UNKNOWN_VALUES)

        assertFalse(device.online)
        assertEquals("4f0c", device.pendingCommandId)
        assertNull(device.updatedAt)
    }

    @Test
    fun `maps a command outcome`() {
        val command = json.decodeFromString<CommandDto>(COMMAND).toDomain()

        assertEquals("4f0c", command.id)
        assertEquals("bf0000000000000000abcd", command.deviceId)
        assertEquals(CommandStatus.ABORTED, command.status)
        assertEquals(CommandReason.OVERRIDDEN, command.reason)
    }

    @Test
    fun `unknown command status becomes null`() {
        val command = json.decodeFromString<CommandDto>(COMMAND.replace("\"aborted\"", "\"paused\"")).toDomain()

        assertNull(command.status)
    }

    private fun map(deviceJson: String, baseUrl: String = BASE_URL): Device =
        json.decodeFromString<DeviceDto>(deviceJson).toDomain(baseUrl)

    private companion object {
        val json = HttpClientFactory.json

        val DYSON = """
            {
              "id": "XXX-EU-XXX0000A",
              "type": "fanHeater",
              "model": "Dyson Hot+Cool HF1",
              "friendlyName": "Obývačka",
              "image": "/images/dyson-hot-cool-hf1.webp",
              "online": true,
              "updatedAt": "2026-10-26T18:04:11Z",
              "attributes": [
                { "id": "power", "type": "bool", "value": true },
                { "id": "targetTemperature", "type": "number", "unit": "celsius", "value": 21 },
                { "id": "currentTemperature", "type": "number", "unit": "celsius", "value": null },
                { "id": "heating", "type": "bool", "value": true },
                { "id": "mode", "type": "enum", "value": "heat", "options": ["heat", "fan"] },
                { "id": "fanSpeed", "type": "number", "value": 4 },
                { "id": "sleepTimer", "type": "number", "unit": "minutes", "value": null }
              ],
              "functions": [
                { "id": "setPower", "attribute": "power", "input": { "type": "bool" }, "available": true },
                { "id": "setTargetTemperature", "attribute": "targetTemperature",
                  "input": { "type": "number", "min": 12, "max": 32, "step": 1 },
                  "available": false, "unavailableReason": "powerOff" },
                { "id": "setMode", "attribute": "mode",
                  "input": { "type": "enum", "options": ["heat", "fan"] }, "available": true },
                { "id": "setSleepTimer", "attribute": "sleepTimer",
                  "input": { "type": "number", "min": 0, "max": 540, "step": 15, "nullable": true },
                  "available": true }
              ],
              "faults": [],
              "pendingCommand": null,
              "diagnostics": { "protocol": "dyson", "ip": "192.168.0.10" }
            }
        """.trimIndent()

        const val FAULTS_JSON =
            """[ { "code": "tilt" }, { "code": "deviceFault", "detail": "dyson:fs03" }, { "code": "overheated" } ]"""

        /** Everything a newer server may send that this app does not know. */
        val UNKNOWN_VALUES = """
            {
              "id": "bf0000000000000000abcd",
              "type": "radiator",
              "model": "Klarstein Bansin",
              "friendlyName": null,
              "image": null,
              "online": false,
              "updatedAt": "yesterday",
              "attributes": [
                { "id": "power", "type": "bool", "value": false },
                { "id": "humidity", "type": "number", "unit": "percent", "value": 40 },
                { "id": "mode", "type": "enum", "value": "turbo", "options": ["comfort", "turbo", "eco"] },
                { "id": "childLock", "type": "bitmask", "value": 3 }
              ],
              "functions": [
                { "id": "setPower", "attribute": "power", "available": true },
                { "id": "setHumidity", "attribute": "humidity", "input": { "type": "number" }, "available": true },
                { "id": "setMode", "attribute": "mode",
                  "input": { "type": "enum", "options": ["comfort", "turbo", "eco"] }, "available": true },
                { "id": "setChildLock", "attribute": "childLock", "input": { "type": "duration" }, "available": true }
              ],
              "faults": $FAULTS_JSON,
              "pendingCommand": {
                "id": "4f0c", "deviceId": "bf0000000000000000abcd", "requestedBy": "jano", "source": "api",
                "status": "pending", "actions": [ { "function": "setPower", "value": true } ],
                "attempts": 1, "createdAt": "2026-10-26T18:04:11Z", "finishedAt": null, "reason": null
              }
            }
        """.trimIndent()

        val COMMAND = """
            {
              "id": "4f0c",
              "deviceId": "bf0000000000000000abcd",
              "requestedBy": "jano",
              "source": "api",
              "status": "aborted",
              "actions": [ { "function": "setPower", "value": true },
                           { "function": "setTargetTemperature", "value": 22 } ],
              "attempts": 1,
              "createdAt": "2026-10-26T18:04:11Z",
              "finishedAt": "2026-10-26T18:04:14Z",
              "reason": "overridden",
              "groupId": "g1"
            }
        """.trimIndent()
    }
}
