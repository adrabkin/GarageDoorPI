/**
 *  
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Z-Wave Door/Window Sensor with HTTP Button
 *	
 *  Author: Avi Drabkin
 *  Date: 2017-02-20
 *
 *	Special Credit goes to HTTP_Button code from https://github.com/chancsc/SC_SmartThings/blob/master/IR-Remote/device-handler/HTTP_Button.groovy
 *
 *	Done by modifying default Smartthings Z-Wave Door/Windows Sensor, and adding code from HTTP_Button
 *	
 *
 */

metadata {
	definition (name: "Z-wave Sensor HTTP Action", namespace: "adrabkin", author: "adrabkin") {
		capability "Contact Sensor"
		capability "Sensor"
		capability "Battery"
		capability "Configuration"
		capability "Switch"
		attribute "triggerswitch", "string"
		
		command "DeviceTrigger"
		

		fingerprint deviceId: "0x2001", inClusters: "0x30,0x80,0x84,0x85,0x86,0x72"
		fingerprint deviceId: "0x07", inClusters: "0x30"
		fingerprint deviceId: "0x0701", inClusters: "0x5E,0x98"
		fingerprint deviceId: "0x0701", inClusters: "0x5E,0x86,0x72,0x98", outClusters: "0x5A,0x82"
		fingerprint deviceId: "0x0701", inClusters: "0x5E,0x80,0x71,0x85,0x70,0x72,0x86,0x30,0x31,0x84,0x59,0x73,0x5A,0x8F,0x98,0x7A", outClusters:"0x20" // Philio multi+
	}

	preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Empty assumes port 80.", required: false, displayDuringSetup: true)
		input("DevicePathOn", "string", title:"URL Path for ON", description: "Rest of the URL, include forward slash.", displayDuringSetup: true)
		input("DevicePathOff", "string", title:"URL Path for OFF", description: "Rest of the URL, include forward slash.", displayDuringSetup: true)
		input(name: "DevicePostGet", type: "enum", title: "POST or GET", options: ["POST","GET"], defaultValue: "POST", required: false, displayDuringSetup: true)
		section() {
			input("HTTPAuth", "bool", title:"Requires User Auth?", description: "Choose if the HTTP requires basic authentication", defaultValue: false, required: true, displayDuringSetup: true)
			input("HTTPUser", "string", title:"HTTP User", description: "Enter your basic username", required: false, displayDuringSetup: true)
			input("HTTPPassword", "string", title:"HTTP Password", description: "Enter your basic password", required: false, displayDuringSetup: true)
		}
	}

	// simulator metadata
	simulator {
		// status messages
		status "open":  "command: 2001, payload: FF"
		status "closed": "command: 2001, payload: 00"
		status "wake up": "command: 8407, payload: "
	}

	// UI tile definitions
	tiles {
		standardTile("contact", "device.contact", width: 2, height: 2) {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
		}
		standardTile("DeviceTrigger", "device.triggerswitch", width: 2, height: 2, canChangeIcon: true) {
			state "default", label: 'Push', action: "on", backgroundColor: "#ffffff", nextState: "trying"
			state "sending", label: 'Trying', action: "off", backgroundColor: "#79b821", nextState: "off"
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:"", backgroundColors:[
                [value: 20, color: "#ff0000"],
                [value: 35, color: "#fd4e3a"],
                [value: 50, color: "#fda63a"],
                [value: 60, color: "#fdeb3a"],
                [value: 75, color: "#d4fd3a"],
                [value: 90, color: "#7cfd3a"],
                [value: 99, color: "#55fd3a"]
            ]
		}

		main "contact"
		details(["contact", "DeviceTrigger", "battery"])
	}
}

def parse(String description) {
	def result = null
	if (description.startsWith("Err 106")) {
		if (state.sec) {
			log.debug description
		} else {
			result = createEvent(
				descriptionText: "This sensor failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.",
				eventType: "ALERT",
				name: "secureInclusion",
				value: "failed",
				isStateChange: true,
			)
		}
	} else if (description != "updated") {
		def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x30: 1, 0x31: 5, 0x80: 1, 0x84: 1, 0x71: 3, 0x9C: 1])
		if (cmd) {
			result = zwaveEvent(cmd)
		}
	}
	log.debug "parsed '$description' to $result"
	return result
}

def on() {
	log.debug "---ON COMMAND--- ${DevicePathOn}"
    sendEvent(name: "triggerswitch", value: "triggeron", isStateChange: true)
	runCmd(DevicePathOn)
}

def off() {
	log.debug "---OFF COMMAND--- ${DevicePathOff}"
    sendEvent(name: "triggerswitch", value: "triggeroff", isStateChange: true)
    runCmd(DevicePathOff)
}

def runCmd(String varCommand) {
	def host = DeviceIP
	def LocalDevicePort = ''
	if (DevicePort==null) { LocalDevicePort = "80" } else { LocalDevicePort = DevicePort }

	def userpassascii = "${HTTPUser}:${HTTPPassword}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()

	log.debug "The device id configured is: $device.deviceNetworkId"

	def path = varCommand
	log.debug "path is: $path"
	//log.debug "Uses which method: $DevicePostGet"
	def body = "" 
	//log.debug "body is: $body"

	def headers = [:] 
	headers.put("HOST", "$host:$LocalDevicePort")
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	if (HTTPAuth) {
		headers.put("Authorization", userpass)
	}
	log.debug "The Header is $headers"
	def method = "POST"
	try {
		if (DevicePostGet.toUpperCase() == "GET") {
			method = "GET"
			}
		}
	catch (Exception e) {
		settings.DevicePostGet = "POST"
		log.debug e
		log.debug "You must not have set the preference for the DevicePOSTGET option"
	}
	log.debug "The method is $method"
	try {
		def hubAction = new physicalgraph.device.HubAction(
			method: method,
			path: path,
			body: body,
			headers: headers
			)
		log.debug hubAction
		return hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
    
    //sendEvent
    if (varCommand == "off"){
    	sendEvent(name: "switch", value: "off")
        log.debug "Executing OFF"
    } else {
    	sendEvent(name: "switch", value: "on")
        log.debug "Executing ON"
    }
    
}

def updated() {
	def cmds = []
	if (!state.MSR) {
		cmds = [
			command(zwave.manufacturerSpecificV2.manufacturerSpecificGet()),
			"delay 1200",
			zwave.wakeUpV1.wakeUpNoMoreInformation().format()
		]
	} else if (!state.lastbat) {
		cmds = []
	} else {
		cmds = [zwave.wakeUpV1.wakeUpNoMoreInformation().format()]
	}
	response(cmds)
}

def configure() {
	commands([
		zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
		zwave.batteryV1.batteryGet()
	], 6000)
}

def sensorValueEvent(value) {
	if (value) {
		createEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open")
	} else {
		createEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd)
{
	sensorValueEvent(cmd.sensorState)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd)
{
	def result = []
	if (cmd.notificationType == 0x06 && cmd.event == 0x16) {
		result << sensorValueEvent(1)
	} else if (cmd.notificationType == 0x06 && cmd.event == 0x17) {
		result << sensorValueEvent(0)
	} else if (cmd.notificationType == 0x07) {
		if (cmd.v1AlarmType == 0x07) {  // special case for nonstandard messages from Monoprice door/window sensors
			result << sensorValueEvent(cmd.v1AlarmLevel)
		} else if (cmd.event == 0x01 || cmd.event == 0x02) {
			result << sensorValueEvent(1)
		} else if (cmd.event == 0x03) {
			result << createEvent(descriptionText: "$device.displayName covering was removed", isStateChange: true)
			if(!state.MSR) result << response(command(zwave.manufacturerSpecificV2.manufacturerSpecificGet()))
		} else if (cmd.event == 0x05 || cmd.event == 0x06) {
			result << createEvent(descriptionText: "$device.displayName detected glass breakage", isStateChange: true)
		} else if (cmd.event == 0x07) {
			if(!state.MSR) result << response(command(zwave.manufacturerSpecificV2.manufacturerSpecificGet()))
			result << createEvent(name: "motion", value: "active", descriptionText:"$device.displayName detected motion")
		}
	} else if (cmd.notificationType) {
		def text = "Notification $cmd.notificationType: event ${([cmd.event] + cmd.eventParameter).join(", ")}"
		result << createEvent(name: "notification$cmd.notificationType", value: "$cmd.event", descriptionText: text, displayed: false)
	} else {
		def value = cmd.v1AlarmLevel == 255 ? "active" : cmd.v1AlarmLevel ?: "inactive"
		result << createEvent(name: "alarm $cmd.v1AlarmType", value: value, displayed: false)
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd)
{
	def event = createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)
	def cmds = []
	if (!state.MSR) {
		cmds << command(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
		cmds << "delay 1200"
	}
	if (!state.lastbat || now() - state.lastbat > 53*60*60*1000) {
		cmds << command(zwave.batteryV1.batteryGet())
	} else {
		cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
	}
	[event, response(cmds)]
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} has a low battery"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
	}
	state.lastbat = now()
	[createEvent(map), response(zwave.wakeUpV1.wakeUpNoMoreInformation())]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def result = []

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	log.debug "msr: $msr"
	updateDataValue("MSR", msr)

	retypeBasedOnMSR()

	result << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)

	if (msr == "011A-0601-0901") {  // Enerwave motion doesn't always get the associationSet that the hub sends on join
		result << response(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId))
	} else if (!device.currentState("battery")) {
		if (msr == "0086-0102-0059") {
			result << response(zwave.securityV1.securityMessageEncapsulation().encapsulate(zwave.batteryV1.batteryGet()).format())
		} else {
			result << response(command(zwave.batteryV1.batteryGet()))
		}
	}

	result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x25: 1, 0x30: 1, 0x31: 5, 0x80: 1, 0x84: 1, 0x71: 3, 0x9C: 1])
	// log.debug "encapsulated: $encapsulatedCommand"
	if (encapsulatedCommand) {
		state.sec = 1
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	createEvent(descriptionText: "$device.displayName: $cmd", displayed: false)
}

private command(physicalgraph.zwave.Command cmd) {
	if (state.sec == 1) {
		zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	} else {
		cmd.format()
	}
}

private commands(commands, delay=200) {
	delayBetween(commands.collect{ command(it) }, delay)
}

def retypeBasedOnMSR() {
	switch (state.MSR) {
		case "0086-0002-002D":
			log.debug "Changing device type to Z-Wave Water Sensor"
			setDeviceType("Z-Wave Water Sensor")
			break
		case "011F-0001-0001":  // Schlage motion
		case "014A-0001-0001":  // Ecolink motion
		case "014A-0004-0001":  // Ecolink motion +
		case "0060-0001-0002":  // Everspring SP814
		case "0060-0001-0003":  // Everspring HSP02
		case "011A-0601-0901":  // Enerwave ZWN-BPC
			log.debug "Changing device type to Z-Wave Motion Sensor"
			setDeviceType("Z-Wave Motion Sensor")
			break
		case "013C-0002-000D":  // Philio multi +
			log.debug "Changing device type to 3-in-1 Multisensor Plus (SG)"
			setDeviceType("3-in-1 Multisensor Plus (SG)")
			break
		case "0109-2001-0106":  // Vision door/window
			log.debug "Changing device type to Z-Wave Plus Door/Window Sensor"
			setDeviceType("Z-Wave Plus Door/Window Sensor")
			break
		case "0109-2002-0205": // Vision Motion
			log.debug "Changing device type to Z-Wave Plus Motion/Temp Sensor"
			setDeviceType("Z-Wave Plus Motion/Temp Sensor")
			break
	}
}
