from confluent_kafka import Consumer

consumer = Consumer({
    # 接入点
    'bootstrap.servers':'120.48.16.84:9095,120.48.159.11:9095,180.76.99.163:9095',
    # 接入协议
    'security.protocol':'PLAINTEXT',
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