package baidu.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerDemo {
    public static void main(String[] args) {
        // 需要自行配置下面三个参数
        // 接入点-PLAINTEXT
        String access_point = "192.168.32.10:9092,192.168.32.12:9092,192.168.32.11:9092";
        // 主题名称-topic name
        String topic = "test";
        // 消费组id
        String group_id = "test_group";

        // 创建配置类
        Properties properties = new Properties();
        // 设置接入点
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, access_point);
        // Kafka消息的序列化方式
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        // 指定消费组id
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, group_id);
        // enable.auto.commit如果为true，则消费者的偏移量将定期在后台提交。
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        // 重置消费位点策略:earliest、latest、none
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // 设置kafka自动提交offset的频率，默认5000ms，也就是5s
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        // 设置消费者在一次poll中返回的最大记录数
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);
        // 设置消费者两次poll的最大时间间隔
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 1000);

        // 构建KafkaConsumer对象
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);

        //订阅主题
        kafkaConsumer.subscribe(Collections.singleton(topic));

        try{
            //持续消费主题中的消息
            while(true){
                // 构建ConsumerRecord用于接收存储消息
                ConsumerRecords<String, String> kafkaMessage = kafkaConsumer.poll(Duration.ofMillis(5000));
                for (ConsumerRecord<String, String> consumerRecord : kafkaMessage) {
                    // 打印消息具体内容
                    System.out.printf("offset = %d, key = %s, value = %s%n", consumerRecord.offset(), consumerRecord.key(), consumerRecord.value());
                }
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }finally {
            kafkaConsumer.close();
        }
    }
}
