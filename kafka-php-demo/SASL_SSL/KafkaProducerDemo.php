<?php

$conf = new RdKafka\Conf();
// 接入点
$conf->set('metadata.broker.list', '接入点');
// 接入协议
$conf->set('security.protocol','sasl_ssl');
// 证书文件路径（证书文件请参考"接入点查看"文档）
$conf->set('ssl.ca.location',__DIR__.'/ssl.cert/client.truststore.pem');
// SASL 机制
$conf->set('sasl.mechanism','SCRAM-SHA-512');
// SASL 用户名
$conf->set('sasl.username','alice');
// SASL 密码
$conf->set('sasl.password','alice1234!');

$producer = new RdKafka\Producer($conf);

// 指定topic
$topic = $producer->newTopic("topic_name");

for ($i = 0; $i < 10; $i++) {
    // RD_KAFKA_PARTITION_UA自动选择分区，message指定想要发送的消息
    $topic->produce(RD_KAFKA_PARTITION_UA, 0, "Message $i : php sasl_ssl");
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