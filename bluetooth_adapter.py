import csv
from time import time
import serial

ser = serial.Serial('/dev/ttyACM0', timeout=1)

f = open("sensor_data.csv", "a+")
writer = csv.writer(f, delimiter=',')

headers = ['Timestamp', 'Temperature', 'Pressure', 'Proximity', 'R', 'B', 'G', 'A', 'xAcc', 'yAcc', 'zAcc']
writer.writerow(headers)

while True:
    s = ser.readline().decode()
    if s != "":
        rows = [float(x) for x in s.split(',')]
        rows.insert(0, int(time()))
        print(rows)
        writer.writerow(rows)
        f.flush()