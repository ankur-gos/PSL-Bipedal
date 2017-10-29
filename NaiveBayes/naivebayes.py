import numpy as np
from sklearn.model_selection import KFold
from sklearn.naive_bayes import GaussianNB
from sklearn.naive_bayes import MultinomialNB
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import fbeta_score, make_scorer
from sklearn import preprocessing
from sklearn.model_selection import LeaveOneOut
from sklearn.model_selection import GridSearchCV, cross_val_score
from sklearn.preprocessing import OneHotEncoder

def run(dataset):
    with open(dataset, 'r') as rf:
        rf.readline()
        data = np.loadtxt(rf, delimiter='\t', dtype=str)
        X = data[:, 0:-1]
        y = data[:, -1]
        le = preprocessing.LabelEncoder()
        le.fit(X[:,0])
        X = le.transform(X[:,0])
        X = X.reshape(-1, 1)
        X = X.astype(float)
        enc = OneHotEncoder()
        X = enc.fit_transform(X)
        y = y.astype(float)
        print np.count_nonzero(y)
        kf_inner = KFold(n_splits=10, shuffle=True)
        kf_outer = KFold(n_splits=10, shuffle=True)
        param_grid = {'alpha': [0.001, 0.01, 0.1, 1, 10, 100]}
        ftwo_scorer = make_scorer(fbeta_score, beta=2)
        model = GridSearchCV(MultinomialNB(), param_grid, scoring=ftwo_scorer, cv=kf_inner)
        model.fit(X, y)
        nested_score = cross_val_score(model, X=X, y=y, scoring=ftwo_scorer, cv=kf_outer)
        print 'Naive Bayes F2: ' + str(nested_score.mean())

        param_grid = {'C': [0.001, 0.01, 0.1, 1, 10, 100, 1000] }
        model = GridSearchCV(LogisticRegression(penalty='l2'), param_grid, scoring=ftwo_scorer, cv=kf_inner)
        model.fit(X, y)
        nested_score = nested_score = cross_val_score(model, X=X, y=y, scoring=ftwo_scorer, cv=kf_outer)
        print 'Logit F2: ' + str(nested_score.mean())
        # betas = []
        # for train, test in kf.split(X):
        #     X_train, X_test, y_train, y_test = X[train], X[test], y[train], y[test]
            
            
        #     model = GridSearchCV(MultinomialNB(), param_grid, scoring=ftwo_scorer)
        #     #model = MultinomialNB()
        #     model.fit(X_train, y_train)
        #     output = model.predict(X_test)
        #     betas.append(fbeta_score(y_test, output, beta=2))
        # print 'Naive Bayes F2: ' + str(np.mean(betas))
        # betas = []
        # for train, test in kf.split(X):
        #     X_train, X_test, y_train, y_test = X[train], X[test], y[train], y[test]
        #     ftwo_scorer = make_scorer(fbeta_score, beta=2)
        #     param_grid = {'C': [0.001, 0.01, 0.1, 1, 10, 100, 1000] }
        #     model = GridSearchCV(LogisticRegression(penalty='l2'), param_grid, scoring=ftwo_scorer)
        #     model.fit(X_train, y_train)
        #     output = model.predict(X_test)
        #     betas.append(fbeta_score(y_test, output, beta=0.5))
        # print 'Logit F2: ' + str(np.mean(betas))
        # betas = []
        # for train, test in kf.split(X):
        #     X_train, X_test, y_train, y_test = X[train], X[test], y[train], y[test]
        #     model = MultinomialNB()
        #     model.fit(X_train, y_train)
        #     output = model.predict(X_test)
        #     betas.append(fbeta_score(y_test, output, beta=2))
        # print np.mean(betas)

def run_trip(dataset, names):
    with open(dataset, 'r') as rf:
        rf.readline()
        data = np.loadtxt(rf, delimiter='\t', dtype=str)
        # cols = []
        # for col in data.dtype.names:
        #     if col == 'Label':
        #         cols.append(data[col])
        #         continue
        #     le = preprocessing.LabelEncoder()
        #     le.fit(data[col])
        #     cols.append(le.transform(data[col]))
        # data = np.column_stack(cols)
        X = data[:, 0:-1]
        y = data[:, -1]
        y = y.astype(float)
        for i in range(X.shape[1]):
            le = preprocessing.LabelEncoder()
            le.fit(X[:,i])
            X[:,i] = le.transform(X[:,i])
        X = X.astype(float)
        enc = OneHotEncoder()
        X = enc.fit_transform(X)
        # le2 = preprocessing.LabelEncoder()
        # le2.fit(X[:,1])
        # X[:,1] = le2.transform(X[:,1])
        kf_inner = KFold(n_splits=10, shuffle=True)
        kf_outer = KFold(n_splits=10, shuffle=True)
        param_grid = {'alpha': [0.001, 0.01, 0.1, 1, 10, 100]}
        ftwo_scorer = make_scorer(fbeta_score, beta=2)
        model = GridSearchCV(MultinomialNB(), param_grid, scoring=ftwo_scorer, cv=kf_inner)
        model.fit(X, y)
        nested_score = cross_val_score(model, X=X, y=y, scoring=ftwo_scorer, cv=kf_outer)
        print 'Naive Bayes F2: ' + str(nested_score.mean())

        param_grid = {'C': [0.001, 0.01, 0.1, 1, 10, 100, 1000] }
        model = GridSearchCV(LogisticRegression(penalty='l2'), param_grid, scoring=ftwo_scorer, cv=kf_inner)
        model.fit(X, y)
        nested_score = nested_score = cross_val_score(model, X=X, y=y, scoring=ftwo_scorer, cv=kf_outer)
        print 'Logit F2: ' + str(nested_score.mean())
        # kf = KFold(n_splits=5)
        # betas = []
        # for train, test in kf.split(X):
        #     X_train, X_test, y_train, y_test = X[train], X[test], y[train], y[test]
        #     ftwo_scorer = make_scorer(fbeta_score, beta=2)
        #     param_grid = {'alpha': [0.001, 0.01, 0.1, 1, 10, 100]}
        #     model = GridSearchCV(MultinomialNB(), param_grid, scoring=ftwo_scorer)
        #     #model = MultinomialNB()
        #     model.fit(X_train, y_train)
        #     output = model.predict(X_test)
        #     betas.append(fbeta_score(y_test, output, beta=2))
        # print 'Naive Bayes F2: ' + str(np.mean(betas))
        # betas = []
        # for train, test in kf.split(X):
        #     X_train, X_test, y_train, y_test = X[train], X[test], y[train], y[test]
        #     ftwo_scorer = make_scorer(fbeta_score, beta=2)
        #     param_grid = {'C': [0.001, 0.01, 0.1, 1, 10, 100, 1000] }
        #     model = GridSearchCV(LogisticRegression(penalty='l2'), param_grid, scoring=ftwo_scorer)
        #     model.fit(X_train, y_train)
        #     output = model.predict(X_test)
        #     betas.append(fbeta_score(y_test, output, beta=0.5))
        # print 'Logit F2: ' + str(np.mean(betas))