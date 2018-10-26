import subprocess
import json
import threading
import queue
import json
import sys
import os
import random

subprocess.call('g++ --std=c++0x -W -Wall -Wno-narrowing -O2 -s -pipe -mmmx -msse -msse2 -msse3 -o out/main.out {}'.format(sys.argv[1]), shell=True)
subprocess.call('javac -d out src/ProductInventoryVis.java', shell=True)

scorefile = "best-score.json"
start = 5
case = 100
end = start + case
scores = [0]

try:
    with open(scorefile, "r") as f:
        scores = json.loads(f.read())
except FileNotFoundError:
    scores = [1] * end

if end > len(scores):
    scores = [1] * end

def solve(seed):
    return int(subprocess.check_output('java -cp out ProductInventoryVis -exec out/main.out -seed {}'.format(seed), shell=True))

def getParam():
    with open("param", "r") as f:
        p = []
        for l in f:
            p.append(int(l))
        return p

def putParam(p):
    with open("param", "w") as f:
        for v in p:
            f.write(str(v) + "\n")

class State:
    count = 0
    rate = 0
    lock = threading.Lock()

    def add(self, seed, score):
        if scores[seed] < score:
            scores[seed] = score
        nom = score / scores[seed]
        with self.lock:
            self.count = self.count + 1
            self.rate = (self.rate * (self.count - 1) + nom) / self.count
            if self.count % 100 == 0:
                print('{}\t{:6d}\t{:4.3f}\t{:4.3f}'.format(seed, score, nom, self.rate))

def worker():
    while True:
        seed = q.get()
        if seed is None:
            break
        try:
            score = solve(seed)
            state.add(seed, score)
            q.task_done()
        except ValueError as err:
            print("seed {} : {}".format(seed, err))
            os._exit(1)

rate = 0
para = getParam()

while True:
    index = random.randrange(160)
    value = random.randrange(2)
    prev = para[index]
    if rate > 0:
        para[index] = prev + (1 if value == 0 else -1)
    putParam(para)

    state = State()
    q = queue.Queue()

    num_worker_threads = 4
    threads = []
    for i in range(num_worker_threads):
        t = threading.Thread(target=worker)
        t.start()
        threads.append(t)

    for seed in range(start, start + case):
        q.put(seed)

    q.join()

    for i in range(num_worker_threads):
        q.put(None)
    for t in threads:
        t.join()
    
    print(para)
    print(state.rate)
    if rate < state.rate + 1e-5:
        rate = state.rate
    else:
        para[index] = prev
    putParam(para)
