from confluent_kafka import Consumer

consumer = Consumer({
    # 接入点
    'bootstrap.servers':'xxx.xx.xx.xx:9099',
    # 接入协议
    'security.protocol':'SSL',
    'ssl.endpoint.identification.algorithm':'none',
    # 证书文件路径
    'ssl.ca.location':'ca.pem',
    'ssl.certificate.location':'client.pem',
    'ssl.key.location':'client.key',
    # 消费组id
    'group.id':'test_group',
    'auto.offset.reset':'latest',
    'fetch.message.max.bytes':'1024*512',
})

# 订阅的主题名称
consumer.subscribe(['topic_name'])

while True:
    msg = consumer.poll(1.0)

    if msg is None:
        continue
    if msg.error():
        print("Consumer error: {}".format(msg.error()))
        continue

    print('Received message: {}'.format(msg.value().decode('utf-8')))

consumer.close()