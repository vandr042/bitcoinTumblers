import numpy as np

f = open('5VP.txt', 'r')



i = 0
tstamps = []
addrs = []
conn_dict = {}
for line in f:
	if (line != "**\n"):
		x = line.split(' ')
		if (i < 10):	# First ten
			pass	
		else:
			tstamps.append(int(x[2].rstrip()))
			addrs.append(x[1])
		i = i + 1	
	else:
		conns = zip(tstamps, addrs)
		conn_dict = dict(conns)
		tstamps.sort()
		i = 0
		tstamps = []
		addrs = []
		conn_dict = {}
