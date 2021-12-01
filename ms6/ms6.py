import paho.mqtt.client as mqtt
import aioblescan as aiobs
from aioblescan.plugins import EddyStone
import asyncio
from datetime import datetime
import time
import json

import numpy as np
import matplotlib
import matplotlib.pyplot as plt
import sklearn.datasets as dt
import pandas as pd
from sklearn.preprocessing import StandardScaler


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

            # perform prediction here
            # use the trained model to make prediction
            # TODO: get prediction

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

    def stochastic_gradient_descent(self, max_epochs, threshold, w_init,
                                    obj_func, grad_func, xy,
                                    learning_rate=0.05, momentum=0.8):
        (x_train, y_train) = xy
        w = w_init
        w_history = w
        f_history = obj_func(w, xy)
        delta_w = np.zeros(w.shape)
        i = 0
        diff = 1.0e10
        rows = x_train.shape[0]

        # Run epochs
        while (i < max_epochs):
            # Shuffle rows using a fixed seed to reproduce the results
            np.random.seed(i)
            p = np.random.permutation(rows)

            # Run for each instance/example in training set
            for x, y in zip(x_train[p, :], y_train[p]):
                delta_w = -learning_rate * \
                    grad_func(w, (np.array([x]), y)) + momentum*delta_w
                w = w+delta_w

            i += 1
            w_history = np.vstack((w_history, w))
            f_history = np.vstack((f_history, obj_func(w, xy)))
            diff = np.absolute(f_history[-1]-f_history[-2])

        return w_history, f_history

    def grad_mse(self, w, xy):
        (x, y) = xy
        (rows, cols) = x.shape

        # Compute the output
        o = np.sum(x*w, axis=1)
        diff = y-o
        diff = diff.reshape((rows, 1))
        diff = np.tile(diff, (1, cols))
        grad = diff*x
        grad = -np.sum(grad, axis=0)
        return grad

    def mse(self, w, xy):
        (x, y) = xy

        o = np.sum(x*w, axis=1)
        mse = np.sum((y-o)*(y-o))
        mse = mse/2
        return mse

    def error(self, w, xy):
        (x, y) = xy
        o = np.sum(x*w, axis=1)

        # map the output values to 0/1 class labels
        ind_1 = np.where(o > 0.5)
        ind_0 = np.where(o <= 0.5)
        o[ind_1] = 1
        o[ind_0] = 0
        return np.sum((o-y)*(o-y))/y.size*100

    def runRegression(self):
        df = pd.read_csv(r"model_input.csv")
        X = df[["max_temp", "min_temp", "humidity"]].values  # type: ignore

        scaler = StandardScaler()

        X = scaler.fit_transform(X)
        X = np.asfarray(X, float)

        y = df["steps"].values  # type: ignore
        y = y.reshape(y.shape[0], 1)

        ones_x = np.ones((670, 1))
        X_ = np.concatenate((ones_x, X), axis=1)

        y = np.asfarray(y, float)

        rand = np.random.RandomState(19)
        w_init = rand.uniform(-1, 1, X_.shape[1])*.001

        w_history_stoch, mse_history_stoch = self.stochastic_gradient_descent(
            1000, 0.1, w_init,
            self.mse, self.grad_mse, (X_, y),
            learning_rate=1e-6, momentum=0.7)

        X1 = df[["max_temp"]].values  # type: ignore

        X2 = df[["min_temp"]].values  # type: ignore
        X3 = df[["humidity"]].values  # type: ignore

        y_pred = w_history_stoch[1000][0] + w_history_stoch[1000][1] * \
            X1 + w_history_stoch[1000][2]*X2 + w_history_stoch[1000][3]*X3
        print("Max step:", y_pred.max(), ", Min step: ", y_pred.min())


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
    mq.runRegression()

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
