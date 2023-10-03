import time
from paho.mqtt import client as client_mqtt
import serial
import os
from dotenv import load_dotenv 

load_dotenv()

serial_port = os.environ['SERIAL_PORT']
host = os.environ['HOST']
client_id = os.environ['CLIENT_MQTT']
username = os.environ['USERNAME']
password = os.environ['PASSWORD']
asset_id = os.environ['ASSET_ID']
publish_topic = os.environ['PUBLISH_TOPIC']
subscribe_topic = os.environ['SUBSCRIBE_TOPIC']
temperature_topic = os.environ['TEMPERATURE_TOPIC']
pressure_topic = os.environ['PRESSURE_TOPIC']
proximity_topic = os.environ['PROXIMITY_TOPIC']
temperature_topic = os.environ['TEMPERATURE_TOPIC']
accX_topic = os.environ['ACCELERATION_X_TOPIC']
accY_topic = os.environ['ACCELERATION_Y_TOPIC']
accZ_topic = os.environ['ACCELERATION_Z_TOPIC']
red_topic = os.environ['COLOR_RED_TOPIC']
blue_topic = os.environ['COLOR_BLUE_TOPIC']
green_topic = os.environ['COLOR_GREEN_TOPIC']
external_conditions_topic = os.environ['EXTERNAL_CONDITIONS_TOPIC']
position_topic = os.environ['POSITION_TOPIC']
message_topic = os.environ['MESSAGE_TOPIC']

ser = serial.Serial(serial_port, timeout=1)

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected to MQTT broker")
    else:
        print("Failed to connect to MQTT broker")



def on_disconnect(client, userdata, rc):
    print('disconnecting reason ' + str(rc))
    client.disconnected_flag = True
    client.connected_flag = False

def on_publish(client, userdata, result):
    print('data published \n')
    pass

def on_message(client, userdata, msg):
    print('received message =', msg)

client_mqtt.Client.connected_flag = False
client = client_mqtt.Client(client_id)
client.username_pw_set(username, password)
client.on_connect = on_connect
client.on_message = on_message
client.on_publish = on_publish
client.on_disconnect = on_disconnect

print('Connecting to broker')
client.connect(host)
# client.loop_start()
# print('in loop')
# client.loop_stop()
try:
    while True:
        s = ser.readline().decode()
        if s != "":
            rows = [float(x) for x in s.split(',')]
            print(rows)

            temperature = rows[0]
            pressure = rows[1]
            proximity = rows[2]
            red = rows[3]
            blue = rows[4]
            green = rows[5]
            aX = rows[7]
            aY = rows[8]
            aZ = rows[9]

            client.publish(publish_topic + temperature_topic + asset_id, temperature)
            client.publish(publish_topic + pressure_topic + asset_id, pressure)
            client.publish(publish_topic + proximity_topic + asset_id, proximity)
            client.publish(publish_topic + red_topic + asset_id, red)
            client.publish(publish_topic + blue_topic + asset_id, blue)
            client.publish(publish_topic + green_topic + asset_id, green)
            client.publish(publish_topic + accX_topic + asset_id, aX)
            client.publish(publish_topic + accY_topic + asset_id, aY)
            client.publish(publish_topic + accZ_topic + asset_id, aZ)

            client.subscribe(subscribe_topic + external_conditions_topic + asset_id)
            client.subscribe(subscribe_topic + position_topic + asset_id)
            client.subscribe(subscribe_topic + message_topic + asset_id)
            
except KeyboardInterrupt:
    print("Disconnecting")
    client.disconnect()