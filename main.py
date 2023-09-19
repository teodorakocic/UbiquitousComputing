import time
# import random
# from paho.mqtt import client as mqtt_client


# def on_connect(client, userdata, flags, rc):
#     if rc == 0:
#         print("Connected to MQTT Broker!")
#     else:
#         print("Failed to connect, return code %d\n", rc)



# def publish(client):
#     time.sleep(1)
#     msg = 25
#     result = client.publish(topic, msg)
#     status = result[0]
#     if status == 0:
#         print(f"Send `{msg}` to topic `{topic}`")
#     else:
#         print(f"Failed to send message to topic {topic}")



# broker = 'mqtt://localhost'
# port = 1883
# client_id = 'client-openr'
# topic = "master/client-openr/writeattributevalue/writeTemperature/7Dd8mox7AdUTOfJnaXorUr"
# username = 'master:mqttuser'
# password = 'uYMIA1IJnrbqreFtY5VomwmBVl7WI1Wc'

# client = mqtt_client.Client(client_id)
# client.username_pw_set(username, password)
# client.on_connect = on_connect
# client.connect(broker, port)

# client.loop_start()
# publish(client)
# client.loop_stop()


from paho.mqtt import client as mqtt_client


def on_connect(client, userdata, flags, rc):
    time.sleep(1)
    print('publishing')
    client.publish('master/client-openr/writeattributevalue/writeTemperature/7Dd8mox7AdUTOfJnaXorUr', 25)
    time.sleep(1)


def on_disconnect(client, userdata, rc):
    print("disconnecting reason  " + str(rc))
    client.connected_flag=False
    client.disconnect_flag=True


def on_message(client, userdata, msg):
    time.sleep(1)
    print("received message =", json.loads(str(msg.payload.decode("utf-8"))))


def on_publish(client,userdata,result):           
    print("data published \n")
    pass

mqtt_client.Client.connected_flag = False
client = mqtt_client.Client("client-openr")
client.username_pw_set("master:mqttuser", "uYMIA1IJnrbqreFtY5VomwmBVl7WI1Wc")
client.on_connect = on_connect
client.on_message = on_message
client.on_publish = on_publish
client.on_disconnect = on_disconnect

print("Connecting to broker")
client.connect("localhost")
client.loop_start()
print("in loop")
client.loop_stop()