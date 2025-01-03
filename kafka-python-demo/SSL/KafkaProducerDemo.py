from confluent_kafka import Producer

producer = Producer({
    # 接入点
    'bootstrap.servers':'xxx.xx.xx.xx:9099',
    # 接入协议
    'security.protocol':'SSL',
    'ssl.endpoint.identification.algorithm': 'none',
    # 证书文件路径
    'ssl.ca.location':'ca.pem',
    'ssl.certificate.location':'client.pem',
    'ssl.key.location':'client.key'
})

def callback_msg(err, msg):
    if err is not None:
        print('send failed:{}'.format(err))
    else:
        print('send success:{}'.format(msg.topic(),msg.partition()))

for _ in range(100):
    # 第一个参数topic_name填写创建的主题名称，第二个参数message写需要发送的消息内容
    producer.produce('topic_name', "message".encode('utf-8'), callback = callback_msg)
    producer.poll(0)

producer.flush()