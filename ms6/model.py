import numpy as np
from numpy.random import seed


class OriginalModel():
    def __init__(self, w0, w1, w2, w3):
        self.w0 = w0
        self.w1 = w1
        self.w2 = w2
        self.w3 = w3
        
    def predict(self, X):
        return (self.w0 + self.w1*X[0] + self.w2*X[1] + self.w3*X[2])


class AdalineSGD(object):
    def __init__(self, eta=0.01, n_iter=10, shuffle=True, random_state=None):
        self.eta = eta
        self.n_iter = n_iter
        self.w_initialized = False
        self.shuffle = shuffle
        if random_state:
            seed(random_state)
        
    def fit(self, X, y):
        X = np.array(X)
        y = np.array(y)
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
        X = np.array(X)
        y = np.array(y)
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
        X = np.array(X)
        y = np.array(y)
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
        absX = np.array(X)
        """Calculate net input"""
        return np.dot(X, self.w_[1:]) + self.w_[0]

    def activation(self, X):
        """Compute linear activation"""
        return self.net_input(X)

    def predict(self, X):
        X = np.array(X)
        """Return class label after unit step"""
        return np.where(self.activation(X) >= 0.0, 1, -1)


test_iterations = 30
previous_data = np.genfromtxt('model_input.csv', delimiter=',')
incoming_data = np.genfromtxt('incoming_data.csv', delimiter=',')

w0 = 718.91565187
w1 = -18.87648056
w2 = -25.9855236
w3 = -27.87443884

origin_clf = OriginalModel(w0, w1, w2, w3)
new_clf = AdalineSGD()

# train model initially
X_prev = [x[:-1] for x in previous_data]
y_prev = [x[len(x)-1:] for x in previous_data]

# remove header row
# change types
X_prev = [list(map(float, x)) for x in X_prev[1:]]

# change type of y to int
y_prev = [int(y[0]) for y in y_prev[1:]]

# train model
new_clf.fit(X_prev, y_prev)
