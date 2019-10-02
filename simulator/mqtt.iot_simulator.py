#!/usr/bin/env python
"""a simple sensor data generator that sends to an MQTT broker via paho"""
import sys
import json
import time
import random
import paho.mqtt.client as mqtt

def generate(host, port, username, password, topic, machines, interval_ms, verbose, yield_ratio):
    """generate data and send it to an MQTT broker"""
    mqttc = mqtt.Client()

    if username:
        mqttc.username_pw_set(username, password)

    mqttc.connect(host, port)

    interval_secs = interval_ms / 1000.0

    random.seed()

    while True:
        data = {
            "machine_id": random.randint(1,100),
            "sensor_ts": long(time.time()*1000000)
        }

        inject_error = (random.randint(1,100) >=  yield_ratio)
        if inject_error:
            data["sensor_0"] = random.randint(51,100)
        else:
            data["sensor_0"] = random.randint(0,50)

        for key in range(1, 11):
            min_val, max_val = machines.get("sensor_" + str(key))
            data["sensor_" + str(key)] = random.randint(min_val, max_val)
            if inject_error and key < 3:
                data["sensor_" + str(key)] *= random.randint(3,7)

        payload = json.dumps(data)

        if verbose:
            print("%s: %s" % (topic, payload))

        mqttc.publish(topic, payload)
        time.sleep(interval_secs)


def main(config_path,plant_id,yield_ratio):
    """main entry point, load and validate config and call generate"""
    try:
        with open(config_path) as handle:
            config = json.load(handle)
            mqtt_config = config.get("mqtt", {})
            misc_config = config.get("misc", {})
            machines = config.get("machines")

            interval_ms = misc_config.get("interval_ms", 500)
            verbose = misc_config.get("verbose", False)

            host = mqtt_config.get("host", "localhost")
            port = mqtt_config.get("port", 1883)
            username = mqtt_config.get("username")
            password = mqtt_config.get("password")
            topic = mqtt_config.get("topic", "mqttgen") + "/" + plant_id

            generate(host, port, username, password, topic, machines, interval_ms, verbose, int(yield_ratio))

    except IOError as error:
        print("Error opening config file '%s'" % config_path, error)

if __name__ == '__main__':
    if len(sys.argv) == 4:
        main(sys.argv[1],sys.argv[2],sys.argv[3])
    else:
        print("usage %s config.json plant_id yield_ratio" % sys.argv[0])
