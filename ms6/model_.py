import numpy as np
import time
import random

class newModel():
    def __init__(self, w0, w1, w2, w3):
        self.w0 = 213.9823764
        self.w1 = -12.87648056
        self.w2 = -20.98552443
        self.w3 = -13.87443823
        
    def predict(self, X):
        return (self.w0 + self.w1*X[0] + self.w2*X[1] + self.w3*X[2])
    
    def update_weights(self):
        w0_update = random.randint(-10, 10)
        w1_update = random.randint(-10, 10)
        w2_update = random.randint(-10, 10)
        w3_update = random.randint(-10, 10)
        
        self.w0 += w0_update
        self.w1 += w1_update
        self.w2 += w2_update
        self.w3 += w3_update

class OriginalModel():
    def __init__(self, w0, w1, w2, w3):
        self.w0 = w0
        self.w1 = w1
        self.w2 = w2
        self.w3 = w3
        
    def predict(self, X):
        return (self.w0 + self.w1*X[0] + self.w2*X[1] + self.w3*X[2])

test_iterations = 10
previous_data = np.genfromtxt('model_input.csv', delimiter=',')
incoming_data = np.genfromtxt('incoming_data.csv', delimiter=',')

w0 = 718.91565187
w1 = -18.87648056
w2 = -25.9855236
w3 = -27.87443884

old_clf = OriginalModel(w0, w1, w2, w3)
new_clf = newModel(w0_new, w1_new, w2_new, w3_new)

X_prev = [x[:-1] for x in previous_data]
y_prev = [x[len(x)-1:] for x in previous_data]

# remove header row
# change types
X_prev = [list(map(float, x)) for x in X_prev[1:]]

# change type of y to int
y_prev = [int(y[0]) for y in y_prev[1:]]

X_incom = [x[:-1] for x in incoming_data]
y_incom = [x[len(x)-1:] for x in incoming_data]

# remove header row
# change types
X_incom = [list(map(float, x)) for x in X_incom[1:]]

# change type of y to int
y_incom = [int(y[0]) for y in y_incom[1:]]


for i in range(test_iterations):
    # get original model prediction
    x1 = X_incom[i][0]
    x2 = X_incom[i][1]
    x3 = X_incom[i][2]
    
    orig_pred = old_clf.predict(X_incom[i])
    print("The original model will predict:", round(abs(orig_pred),0), "steps")
    time.sleep(1)
    
    print("\nThe weights for personalized model are:")
    print("w0 =", new_clf.w0)
    print("w1 =", new_clf.w1)
    print("w2 =", new_clf.w2)
    print("w3 =", new_clf.w3)
    time.sleep(1)
    
    new_pred = new_clf.predict(X_incom[i])
    new_clf.update_weights()
    
    print("The new model will predict:", round(abs(new_pred),0), "steps\n")
