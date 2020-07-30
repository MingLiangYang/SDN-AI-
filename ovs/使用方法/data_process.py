#将内核态数据分别写入不同的文件中
with open("kern.log","r",encoding='UTF-8',errors='ignore') as f_kernal,open("datapath_Gary","a+") as f_datapath_Gary,open("datapath_upcall","a+") as f_datapath_upcall:
    while 1:
        line_data=f_kernal.readline()
        if not line_data:
            break
        if len(line_data.split("Gary:"))>1:
            data=line_data.split("Gary:")[1]
            f_datapath_Gary.write(data)
        elif len(line_data.split("upcall:"))>1:
            data=line_data.split("upcall:")[1]
            f_datapath_upcall.write(data)
#处理用户态数据
with open("gary.log","r",encoding='UTF-8',errors='ignore') as f_kernal,open("user_upcall","a+") as f_user_upcall,\
open("user_table_time","a+") as f_user_table_time,open("userspace","a+") as f_userspace:
    while 1:
        line_data=f_kernal.readline()
        if not line_data:
            break
        if len(line_data.split("gary_upcall:"))>1:
            data=line_data.split("gary_upcall:")[1]
            f_user_upcall.write(data)
        elif len(line_data.split("userspace:"))>1:
            data=line_data.split("userspace:")[1]
            f_userspace.write(data)
        elif len(line_data.split("user_table_time:"))>1:
            data=line_data.split("user_table_time:")[1]
            f_user_table_time.write(data)