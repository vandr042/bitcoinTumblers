import re
import math

def contains(l,n): #check if already in list
    try:
        l.index(n)
        return 0
    except ValueError as e:
        return -1

addrDict = dict()
f = open('/home/connor/workspace/bitcoinTumblers/bitcoinT/harvest.log', 'r')
wFile = open('testWrite.txt','w')
s = f.readline()
badCount = 0
z = 0

#extract addresses from file
while(z < 1000):
    m = re.findall(r'(\d{0,5}\.\d{0,5}\.\d{0,5}\.\d{0,5}\]\:\d{4})', s)
    if (len(m) < 2): # check if bad address
            badCount += 1
    else:
        for i in range(len(m)):
            m[i] = re.sub(r'\]',"",m[i])
        if m[0] in addrDict:
            #Is peer already listed as knowing about this address?
            knownAddr = contains(addrDict[m[0]], m[1])      
            if knownAddr == -1:         # if not in list append
                addrDict[m[0]].append(m[1])
        else:
            #if first time seeing address, add to dict
            addrDict[m[0]] = [m[1]]
    z+=1
    s = f.readline()

print("done")
print(badCount) #bad addresses

#writing values to file for test
y = 0
for index,key in enumerate(addrDict):
    wFile.write(str(addrDict[key]) + '\n')
    y+=1
    if y == 500:
        break

wFile.close()











