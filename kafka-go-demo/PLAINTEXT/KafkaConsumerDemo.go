package PLAINTEXT

import (
	"fmt"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func main() {

	var kafkaconf = &kafka.ConfigMap{
		// 接入点
		"bootstrap.servers": "接入点",
		// 接入协议
		"security.protocol": "plaintext",
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
