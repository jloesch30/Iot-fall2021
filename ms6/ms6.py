import paho.mqtt.client as mqtt
import aioblescan as aiobs
from aioblescan.plugins import EddyStone
import asyncio
from datetime import datetime
import time
import json

class MQTTBroker:
    def __init__(self, client, broker_address, publishTopic, publishTopicGoal, btctrl):
        self.client = client
        self.broker_address = broker_address
        self.publishTopic = publishTopic
        self.publishTopicGoal = publishTopicGoal
        self.btctrl = btctrl
        self.connected = False
        self.stepTracker = 0
        self.stepGoalToday = 0
        self.stepGoalTomorrow = 0

    def _send_message(self, data):
        print("data being published is: {}".format(str(data)))
        self.client.publish(self.publishTopic, data) 

    def _process_packet(self, data):
        ev = aiobs.HCI_Event()
        xx = ev.decode(data)
        xx = EddyStone().decode(ev)
        try:
            if xx:
                # mac address of the microbit
                if xx['mac address'] == "ca:13:b1:40:6c:e9":
                    print_str = "Step count received at {} is {}".format(datetime.now().strftime("%A, %B %d at %I %p"), xx['url'].split('/')[1]) 
                    print(print_str) 

                    # publish topic data here
                    print("publishing")
                    self._send_message(print_str)

                    # check if the day has reset
                    if (xx['url'].split('/')[1] == "reset"):
                        self.stepTracker = 0

                    else:
                        incomingSteps = int(xx['url'].split('/')[1])
                        self.stepTracker = incomingSteps
                        
                        if self.stepGoalToday == 0:
                            print("Steps have not been calculated for today")
                        if self.stepGoalTomorrow == 0:
                            print("Steps have not been calculated for tomorrow")
                        if not self.stepGoalToday == 0 and not self.stepGoalTomorrow == 0:
                            print("Your current steps are: {}\nYour goal for today is: {}\nYour goal for tomorrow is: {}".format(self.stepTracker, self.stepGoalToday, self.stepGoalTomorrow))
        except Exception as e:
            print(e)

    def _client_close(self):
        self.client.loop_stop()

    def _bt_close(self):
        self.btctrl.stop_scan_request()

    def _on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            print("Connected to broker")
            self.connected = True

        else:
            print("Connection failed")
        
    def _client_subscribe(self, topic):
        self.client.subscribe(topic)
        print("Subscribing to " + topic)

    def _on_message(self, client, userData, message):
        try:
            print("Message incoming from topic: " + str(message.topic))

            # do linear regression here
            payload = json.loads(message.payload.decode("utf-8"))

            print(payload)

            max_temp_curr = float(payload["temp_max_curr"])
            min_temp_curr = float(payload["temp_min_curr"])
            humidity_curr = int(payload["humidity_curr"])

            max_temp_tomorrow = float(payload["temp_max_tomorrow"])
            min_temp_tomorrow = float(payload["temp_min_tomorrow"])
            humidity_tomorrow = int(payload["humidity_tomorrow"])

            # linear regression (pre-trained model)
            w0 = 718.91565187
            w1 = -18.87648056
            w2 = -25.9855236
            w3 = -27.87443884

            steps_curr = int(abs(w0 + w1*max_temp_curr + w2*min_temp_curr + w3*humidity_curr))
            steps_tomorrow = int(abs(w0 + w1*max_temp_tomorrow + w2*min_temp_tomorrow + w3*humidity_tomorrow))
            print("The number of steps for today is:", steps_curr)
            print("The number of steps for tomorrow is:", steps_tomorrow)

            # store steps for today
            self.stepGoalToday = steps_curr
            self.stepGoalTomorrow = steps_tomorrow

            # send the data to the topic
            self.client.publish(self.publishTopicGoal, json.dumps({"stepGoalToday": self.stepGoalToday, "stepGoalTomorrow": self.stepGoalTomorrow, "currSteps": self.stepTracker}).encode('utf-8'))

            
        except Exception as e:
            print(e)

    def client_connect(self):
        self.client.on_connect = self._on_connect
        self.client.on_message = self._on_message

        self.client.connect(self.broker_address)
        self.client.loop_start()

    def btControler_connect(self):
        self.btctrl.process = self._process_packet
        self.btctrl.send_scan_request()
            
if __name__ == '__main__':
    mydev = 0
    broker_address = "192.168.4.1"
    weatherTopic  = "android/weatherTopic"
    publishTopic = "microbit/steps"
    publishTopicGoal = "microbit/stepGoal"

    # create class here
    client = mqtt.Client("Python")
    event_loop = asyncio.get_event_loop()
    mysocket = aiobs.create_bt_socket(mydev)
    fac = event_loop._create_connection_transport(mysocket,aiobs.BLEScanRequester,None,None)
    conn, btctrl = event_loop.run_until_complete(fac)

    mq = MQTTBroker(client, broker_address, publishTopic, publishTopicGoal, btctrl)
    mq.btControler_connect()
    mq.client_connect()

    while mq.connected != True:
        time.sleep(0.1)

    mq._client_subscribe(weatherTopic)

    try:
        event_loop.run_forever()
        time.sleep(1)
    except KeyboardInterrupt:
        print('keyboard interrupt')
    finally:
        print('closing event loop')
        mq._bt_close()
        conn.close()
        event_loop.close()
        mq._client_close()
