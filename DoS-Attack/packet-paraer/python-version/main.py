from scapy.all import *
from filewriter import filewriter

def process_pcap(infile,outfile):
    packets = rdpcap(infile)

    for data in packets:
        ethType = '%04x' % data['Ether'].type
        if ethType != '0800':
            continue
        timeArray = time.localtime(data.time)
        otherStyleTime = time.strftime("%Y.%m.%d %H:%M:%S", timeArray)
        srcIp = data['IP'].src
        dstIp = data['IP'].dst
        size = data['IP'].len
        if 'UDP' in data:
            srcPort = data['UDP'].sport
            dstPort = data['UDP'].dport
        elif 'TCP' in data:
            srcPort = data['TCP'].sport
            dstPort = data['TCP'].dport
        else:
            continue
        content = "Time " + str(otherStyleTime) + " src_IP " + str(srcIp) + " dst_IP " + str(dstIp)\
              + " EtherType 0x" + str(ethType) + " srcProt " + str(srcPort) + " dstPort " + str(dstPort)\
              + " size " + str(size)
        filewriter.writeToPath(outfile, content)

if __name__ == '__main__':
    for i in range(0,10):
        print(i)
        infile='a_0000'+str(i)+'.pcap'
        outfile='res_0000'+str(i)+'.txt'
        process_pcap(infile,outfile)
    for i in range(10,20):
        print(i)
        infile='a_000'+str(i)+'.pcap'
        outfile='res_0000'+str(i)+'.txt'
        process_pcap(infile,outfile)

