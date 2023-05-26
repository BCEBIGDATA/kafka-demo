<?php

$conf = new RdKafka\Conf();
// 接入点
$conf->set('metadata.broker.list', '接入点');
// 接入协议
$conf->set('security.protocol','plaintext');


$producer = new RdKafka\Producer($conf);

// 指定topic
$topic = $producer->newTopic("topic_name");

for ($i = 0; $i < 10; $i++) {
    // RD_KAFKA_PARTITION_UA自动选择分区,message指定想要发送的消息
    $topic->produce(RD_KAFKA_PARTITION_UA, 0, "Message $i");
    $producer->poll(0);
}

for ($flushRetries = 0; $flushRetries < 10; $flushRetries++) {
    $result = $producer->flush(10000);
    if (RD_KAFKA_RESP_ERR_NO_ERROR === $result) {
        break;
    }
}

if (RD_KAFKA_RESP_ERR_NO_ERROR !== $result) {
    throw new \RuntimeException('Was unable to flush, messages might be lost!');
}
?>