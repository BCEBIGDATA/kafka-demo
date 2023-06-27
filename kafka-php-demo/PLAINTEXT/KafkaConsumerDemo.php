<?php

$conf = new RdKafka\Conf();
// 接入协议
$conf->set('security.protocol','plaintext');
// 消费组 id
$conf->set('group.id', 'php-group');

$rk = new RdKafka\Consumer($conf);
// 接入点
$rk->addBrokers("接入点");

$topicConf = new RdKafka\TopicConf();
$topicConf->set('auto.commit.interval.ms', 100);
$topicConf->set('offset.store.method', 'broker');
$topicConf->set('auto.offset.reset', 'earliest');

//订阅topic
$topic = $rk->newTopic("topic_name", $topicConf);

$topic->consumeStart(0, RD_KAFKA_OFFSET_STORED);

while (true) {
    $message = $topic->consume(0, 120*10000);
    switch ($message->err) {
        case RD_KAFKA_RESP_ERR_NO_ERROR:
            var_dump($message);
            break;
        case RD_KAFKA_RESP_ERR__PARTITION_EOF:
            echo "No more messages; will wait for more\n";
            break;
        case RD_KAFKA_RESP_ERR__TIMED_OUT:
            echo "Timed out\n";
            break;
        default:
            throw new \Exception($message->errstr(), $message->err);
            break;
    }
}
?>