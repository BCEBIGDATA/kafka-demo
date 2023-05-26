from confluent_kafka import Producer

producer = Producer({
    # 接入点
    'bootstrap.servers':'120.48.16.84:9095,120.48.159.11:9095,180.76.99.163:9095',
    # 接入协议
    'security.protocol':'SASL_PLAINTEXT',
    # 'ssl.endpoint.identification.algorithm': 'none',
    # 'ssl.ca.location':'client.truststore.pem',
    # SASL 机制
    'sasl.mechanism':'SCRAM-SHA-512',
    # SASL 用户名
    'sasl.username':'username',
    # SASL 用户密码
    'sasl.password':'password'
})

def callback_msg(err, msg):
    if err is not None:
        prinf('send failed:{}'.format(err))
    else:
        print('send success:{}'.format(msg.topic(),msg.partition()))

for _ in range(100):
    # 第一个参数topic_name填写创建的主题名称，第二个参数message写需要发送的消息内容
    producer.produce('topic_name', "message".encode('utf-8'), callback = callback_msg)
    producer.poll(0)
    producer.flush()