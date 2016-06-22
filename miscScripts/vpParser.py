import numpy as np

f = open('25VP.txt', 'r')

i = 0
tstamps = []
addrs = []
firstTen = []
conn_dict = {}
numDirFirst = 0.0 # Number of times the first directly connected peer has the 
		# earliest time stamp
correctness = 0.0
for line in f:
	if (line != "**\n"):
		x = line.split(' ')
		if (i < 10):	# First ten
			firstTen.append(x[1])
		else:
			tstamps.append(int(x[2].rstrip()))
			addrs.append(x[1])
		i = i + 1	
	else:
		# loop over tstamps/addrs adding them to the dict if the
		# key does not exist, and handling collisions using 
		# lists of addrs as the values
		for j in range(0, len(tstamps)):
			if tstamps[j] in conn_dict:
				conn_dict[tstamps[j]].append(addrs[j])
			else:
				conn_dict[tstamps[j]] = [addrs[j]]
		tstamps.sort()
		for addr in conn_dict[tstamps[0]]:
			if addr == firstTen[0]:
				numDirFirst = numDirFirst + 1

		# try to build the first ten messages
		myFirstTen = []
		trail = conn_dict[tstamps[0]][0]
		myFirstTen.append(trail)
		k = 1
		j = 0
		while k < 10 and j < len(tstamps):
			t = tstamps[j]
			for addr in conn_dict[t]:
				if trail != addr:
					myFirstTen.append(addr)
					k = k + 1	
					trail = addr
			j = j+1
		numFirstTenCorrect = 0.0
		for j in range(0,len(firstTen)):
			if (myFirstTen[j] == firstTen[j]):
				numFirstTenCorrect = numFirstTenCorrect + 1
		print("myFirstTen length: " + str(len(myFirstTen)))
		print("correctness: " + str(numFirstTenCorrect/10))	
		correctness += numFirstTenCorrect/10	
		numFirstTenCorrect = 0.0	
		#reset variables/data structs for next transaction
		i = 0
		tstamps = []
		addrs = []
		firstTen = []
		conn_dict = {}
print(str(numDirFirst/1000))
print(str(correctness/1000))
