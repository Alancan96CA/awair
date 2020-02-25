/**
 *  Awair
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
 */
public static String version() { return "v0.0.1.20200224" }
/*
 *   2020/02/24 >>> v0.0.1.20200224 - Initialize
 */
import groovy.json.*
import groovy.json.JsonSlurper

metadata {
    definition(name: "Awair-Other-Local", namespace: "ktj1312", author: "ktj1312", vid: "SmartThings-Awair-Local", ocfDeviceType: "x.com.st.d.airqualitysensor") {
        capability "Air Quality Sensor" // Awair Score
        capability "Carbon Dioxide Measurement" // co : clear, detected
        capability "Fine Dust Sensor"
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Tvoc Measurement"
        capability "Illuminance Measurement"
        capability "Sound Pressure Level"
        capability "Sensor"

        command "refresh"
    }

    preferences {
        input "awairAddress", "text", type: "text", title: "어웨어 IP 주소", description: "enter awair address must be [ip]:[port] ", required: true
        input type: "paragraph", element: "paragraph", title: "Version", description: version(), displayDuringSetup: false
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles {
        multiAttributeTile(name: "airQuality", type: "generic", width: 6, height: 4) {
            tileAttribute("device.airQuality", key: "PRIMARY_CONTROL") {
                attributeState('default', label: '${currentValue}')
            }

            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
                attributeState("default", label: 'Update time : ${currentValue}')
            }
        }

        valueTile("temperature_value", "device.temperature") {
            state "default", label: '${currentValue}°'
        }

        valueTile("humidity_value", "device.humidity", decoration: "flat") {
            state "default", label: '${currentValue}%'
        }

        valueTile("co2_value", "device.carbonDioxide", decoration: "flat") {
            state "default", label: '${currentValue}'
        }

        valueTile("voc_value", "device.tvocLevel", decoration: "flat") {
            state "default", label: '${currentValue}'
        }

        valueTile("pm25_value", "device.fineDustLevel", decoration: "flat") {
            state "default", label: '${currentValue}', unit: "㎍/㎥"
        }

        valueTile("lux_value", "device.illuminance", decoration: "flat") {
            state "default", label: '${currentValue}'
        }

        valueTile("spl_value", "device.soundPressureLevel", decoration: "flat") {
            state "default", label: '${currentValue}', unit: "db"
        }

        standardTile("refresh_air_value", "", width: 1, height: 1, decoration: "flat") {
            state "default", label: "", action: "refresh", icon: "st.secondary.refresh"
        }

        main(["airQuality"])
        details([
                "airQuality",
                "temperature_value",
                "humidity_value",
                "co2_value",
                "voc_value",
                "pm25_value",
                "lux_value",
                "spl_value",
                "refresh_air_value"
        ])
    }
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
}

def installed() {
    log.debug "installed()"
    init()
}

def uninstalled() {
    log.debug "uninstalled()"
    unschedule()
}

def updated() {
    log.debug "updated()"
    unschedule()
    init()
}

def init(){
    refresh()
    schedule("0 0/1 * * * ?", refresh)
}

def refresh() {
    log.debug "refresh()"

    if(awairAddress){
        updateAirData()
        def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
        sendEvent(name: "lastCheckin", value: now, displayed: false)
    }
    else log.error "Missing settings awairAddress"
}

def updateAirData(){
    def options = [
            "method": "GET",
            "path": "/air-data/latest",
            "headers": [
                    "HOST": "${awairAddress}"
            ]
    ]

    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: updateAirdataValues])
    sendHubCommand(myhubAction)
}

def updateAirdataValues(physicalgraph.device.HubResponse hubResponse){

    def msg
    try {
        msg = parseLanMessage(hubResponse.description)

        def resp = new JsonSlurper().parseText(msg.body)

        sendEvent(name: "airQuality", value: resp.score)
        sendEvent(name: "temperature", value: resp.temp)
        sendEvent(name: "humidity", value: resp.humid)
        sendEvent(name: "carbonDioxide", value: resp.co2)
        sendEvent(name: "tvocLevel", value: resp.voc)
        sendEvent(name: "fineDustLevel", value: resp.pm25)
        sendEvent(name: "illuminance", value: resp.lux)
        sendEvent(name: "soundPressureLevel", value: resp.spl_a)

    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}