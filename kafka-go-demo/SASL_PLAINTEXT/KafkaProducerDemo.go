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
	}

	p, err := kafka.NewProducer(kafkaconf)
	if err != nil {
		panic(err)
	}

	defer p.Close()

	// Delivery report handler for produced messages
	go func() {
		for e := range p.Events() {
			switch ev := e.(type) {
			case *kafka.Message:
				if ev.TopicPartition.Error != nil {
					fmt.Printf("Delivery failed: %v\n", ev.TopicPartition)
				} else {
					fmt.Printf("Delivered message to %v\n", ev.TopicPartition)
				}
			}
		}
	}()

	// 填写创建的主题名称
	topic := "topic_name"
	for _, word := range []string{"Golang", "for", "kafka", "client", "test"} {
		p.Produce(&kafka.Message{
			TopicPartition: kafka.TopicPartition{Topic: &topic, Partition: kafka.PartitionAny},
			Value:          []byte(word),
		}, nil)
	}

	// Wait for message deliveries before shutting down
	p.Flush(15 * 1000)
}
