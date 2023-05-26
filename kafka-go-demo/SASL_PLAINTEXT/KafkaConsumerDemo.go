package SASL_PLAINTEXT

import (
	"fmt"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func main() {

	var kafkaconf = &kafka.ConfigMap{
		// 接入点
		"bootstrap.servers": "接入点",
		// 接入协议
		"security.protocol": "sasl_plaintext",
		// SASL 机制
		"sasl.mechanism": "SCRAM-SHA-512",
		// SASL 用户名
		"sasl.username": "alice",
		// SASL 用户密码
		"sasl.password": "alice1234!",
		// 消费组 id
		"group.id":          "test_group",
		"auto.offset.reset": "earliest",
	}

	c, err := kafka.NewConsumer(kafkaconf)

	if err != nil {
		panic(err)
	}
	//填写创建的主题的名称
	c.SubscribeTopics([]string{"topic_name"}, nil)

	for {
		msg, err := c.ReadMessage(-1)
		if err == nil {
			fmt.Printf("Message on %s: %s\n", msg.TopicPartition, string(msg.Value))
		} else {
			// The client will automatically try to recover from all errors.
			fmt.Printf("Consumer error: %v (%v)\n", err, msg)
		}
	}

	c.Close()
}
