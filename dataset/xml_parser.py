from xml.dom.minidom import parse
import xml.dom.minidom
import time


'''
<TestbedMonJun14Flows>
<appName>Unknown_UDP</appName>
<totalSourceBytes>16076</totalSourceBytes>
<totalDestinationBytes>0</totalDestinationBytes>
<totalDestinationPackets>0</totalDestinationPackets>
<totalSourcePackets>178</totalSourcePackets>
<sourcePayloadAsBase64></sourcePayloadAsBase64>
<destinationPayloadAsBase64></destinationPayloadAsBase64>
<destinationPayloadAsUTF></destinationPayloadAsUTF>
<direction>L2R</direction>
<sourceTCPFlagsDescription>N/A</sourceTCPFlagsDescription>
<destinationTCPFlagsDescription>N/A</destinationTCPFlagsDescription>
<source>192.168.5.122</source>
<protocolName>udp_ip</protocolName>
<sourcePort>5353</sourcePort>
<destination>224.0.0.251</destination>
<destinationPort>5353</destinationPort>
<startDateTime>2010-06-13T23:57:19</startDateTime>
<stopDateTime>2010-06-14T00:11:23</stopDateTime>
<Tag>Normal</Tag>
</TestbedMonJun14Flows>
'''


def unix_time(dt):
    # 转换成时间数组
    timeArray = time.strptime(dt, "%Y-%m-%dT%H:%M:%S")
    # 转换成时间戳
    timestamp = int(time.mktime(timeArray))
    return timestamp


f = open('res2.txt', mode='a+')

# 使用minidom解析器打开 XML 文档
DOMTree = xml.dom.minidom.parse("TestbedMonJun14Flows.xml")
collection = DOMTree.documentElement
if collection.hasAttribute("dataroot"):
   print ("Root element : %s" % collection.getAttribute("dataroot"))

# 在集合中获取所有电影
flows = collection.getElementsByTagName("TestbedMonJun14Flows")

# 打印每部电影的详细信息
for flow in flows:
   # print ("*****Flow*****")
   tag = flow.getElementsByTagName('Tag')[0].childNodes[0].data
   # print(tag)
   if tag == 'Attack':
      srcIP = flow.getElementsByTagName('source')[0].childNodes[0].data
      dstIP = flow.getElementsByTagName('destination')[0].childNodes[0].data
      srcPort = flow.getElementsByTagName('sourcePort')[0].childNodes[0].data
      dstPort = flow.getElementsByTagName('destinationPort')[0].childNodes[0].data
      starttime = unix_time(flow.getElementsByTagName('startDateTime')[0].childNodes[0].data)
      endtime = unix_time(flow.getElementsByTagName('stopDateTime')[0].childNodes[0].data)
      f.write('%s %s %s %s %s %s\n' % (srcIP, dstIP, srcPort, dstPort, starttime, endtime))

f.close()
   