import paho.mqtt.client as mqtt
import aioblescan as aiobs
from aioblescan.plugins import EddyStone
import asyncio
from datetime import datetime
import time
import json

import numpy as np
from numpy.random import seed

class AdalineSGD(object):
    def __init__(self, eta=0.01, n_iter=10, shuffle=True, random_state=None):
        self.eta = eta
        self.n_iter = n_iter
        self.w_initialized = False
        self.shuffle = shuffle
        if random_state:
            seed(random_state)
        
    def fit(self, X, y):
        """ Fit training data.

        Parameters
        ----------
        X : {array-like}, shape = [n_samples, n_features]
            Training vectors, where n_samples is the number of samples and
            n_features is the number of features.
        y : array-like, shape = [n_samples]
            Target values.

        Returns
        -------
        self : object

        """
        self._initialize_weights(X.shape[1])
        self.cost_ = []
        for i in range(self.n_iter):
            if self.shuffle:
                X, y = self._shuffle(X, y)
            cost = []
            for xi, target in zip(X, y):
                cost.append(self._update_weights(xi, target))
            avg_cost = sum(cost) / len(y)
            self.cost_.append(avg_cost)
        return self

    def partial_fit(self, X, y):
        """Fit training data without reinitializing the weights"""
        if not self.w_initialized:
            self._initialize_weights(X.shape[1])
        if y.ravel().shape[0] > 1:
            for xi, target in zip(X, y):
                self._update_weights(xi, target)
        else:
            self._update_weights(X, y)
        return self

    def _shuffle(self, X, y):
        """Shuffle training data"""
        r = np.random.permutation(len(y))
        return X[r], y[r]
    
    def _initialize_weights(self, m):
        """Initialize weights to zeros"""
        self.w_ = np.zeros(1 + m)
        self.w_initialized = True
        
    def _update_weights(self, xi, target):
        """Apply Adaline learning rule to update the weights"""
        output = self.net_input(xi)
        error = (target - output)
        self.w_[1:] += self.eta * xi.dot(error)
        self.w_[0] += self.eta * error
        cost = 0.5 * error**2
        return cost
    
    def net_input(self, X):
        """Calculate net input"""
        return np.dot(X, self.w_[1:]) + self.w_[0]

    def activation(self, X):
        """Compute linear activation"""
        return self.net_input(X)

    def predict(self, X):
        """Return class label after unit step"""
        return np.where(self.activation(X) >= 0.0, 1, -1)

class MQTTBroker:
    def __init__(self, client, broker_address, publishTopic, publishTopicGoal, btctrl):
        self.client = client
        self.broker_address = broker_address
        self.publishTopic = publishTopic
        self.publishTopicGoal = publishTopicGoal
        self.btctrl = btctrl
        self.model = None
        self.connected = False
        self.stepTracker = 0
        self.stepGoalToday = 0
        self.stepGoalTomorrow = 0
        self.all_entries = [] 

        # initialize ada model
        self.adaclf = AdalineSGD()

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
                    print_str = ("Step count received at {} is {}"
                                 .format(datetime.now().strftime("%A, %B %d at %I %p"), xx['url'].split('/')[1]))
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
                            print("Your current steps are: {}\n\
                            Your goal for today is: {}\nYour goal for tomorrow is: {}"
                                  .format(self.stepTracker, self.stepGoalToday, self.stepGoalTomorrow))
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

            ''' model training '''

            # get data into correct form
            X_dict_today = {"max_temp_curr": max_temp_curr, "min_temp_curr": min_temp_curr, "humidity_curr": humidity_curr}
            X_dict_tomorrow = {"max_temp_tomorrow": max_temp_tomorrow, "min_temp_tomorrow": min_temp_tomorrow, "humidity_tomorrow": humidity_tomorrow}

            # make prediction
            res = self.adaclf.predict(X_dict_today.values())
            print(res)
            pause = input() # DEBUGGING

            # print prediction from new model

            # print prediction from old model
            

            # add the incoming data to the data pool
            # form will look like {"day".. "max_temp" .. "min_temp" .. "steps"}
            
            # get date
            now = datetime.now()

            # new entry
            new_entry = {"date": str(now), "max_temp_curr": max_temp_curr, "min_temp_curr": min_temp_curr, "humidity_curr": humidity_curr, "steps": self.stepTracker} 
            self.all_entries.append(new_entry.values())

            # partial_fit the model with new entry included (getting the last ten entries)
            pre_X = self.all_entries[-10:]
            X = [x[1:-1] for x in pre_X] # skipping the "day" column and "steps" column
            y = [x[len(x)-1:] for x in pre_X] # getting only step column
            self.adaclf.partial_fit(X, y)

            # send the data to the topic
            self.client.publish(self.publishTopicGoal, json.dumps(
                {"stepGoalToday": self.stepGoalToday, "stepGoalTomorrow": self.stepGoalTomorrow, "currSteps": self.stepTracker}).encode('utf-8'))

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
    weatherTopic = "android/weatherTopic"
    publishTopic = "microbit/steps"
    publishTopicGoal = "microbit/stepGoal"

    # create class here
    client = mqtt.Client("Python")
    event_loop = asyncio.get_event_loop()
    mysocket = aiobs.create_bt_socket(mydev)
    fac = event_loop._create_connection_transport(  # type: ignore
        mysocket, aiobs.BLEScanRequester, None, None)
    conn, btctrl = event_loop.run_until_complete(fac)

    mq = MQTTBroker(client, broker_address, publishTopic,
                    publishTopicGoal, btctrl)
    mq.btControler_connect()
    mq.client_connect()

    # run model
    print("running regression...")

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
