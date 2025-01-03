using System;
using Confluent.Kafka;

class Producer
{
    public static void Main(string[] args)
    {
        var conf = new ProducerConfig {
            // 接入点
            BootstrapServers = "接入点",
            // SSL证书文件路径
            SslCaLocation = "kafka-key/ca.pem",
            // SASL机制
            SaslMechanism = SaslMechanism.ScramSha512,
            // 接入协议
            SecurityProtocol = SecurityProtocol.SaslSsl,
            // 用户名
            SaslUsername = "用户名",
            // 密码
            SaslPassword = "密码",
            SslEndpointIdentificationAlgorithm = SslEndpointIdentificationAlgorithm.None,
            };

        Action<DeliveryReport<Null, string>> handler = r =>
            Console.WriteLine(!r.Error.IsError
                ? $"Delivered message to {r.TopicPartitionOffset}"
                : $"Delivery Error: {r.Error.Reason}");

        // 指定主题
        string topic ="主题名称";

        using (var p = new ProducerBuilder<Null, string>(conf).Build())
        {
            for (int i=0; i<100; ++i)
            {
                p.Produce(topic, new Message<Null, string> { Value = i.ToString() }, handler);
            }
            p.Flush(TimeSpan.FromSeconds(10));
        }
    }
}