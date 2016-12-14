# jepsen.zookeeper

Our copy of the jepsen.zookeeper test project, constructed as we worked through a great 2 day tutorial provided by Kyle Kingsbury.

Zookeeper claims to offer "Linearizable Writes" with "Sequential Reads" consistency:

![db-consistency-family-tree](https://cloud.githubusercontent.com/assets/2184330/21183778/67fbdcbe-c201-11e6-924a-c7235128db69.jpg)

( "strongest" at the top, "weaker" at the bottom. https://aphyr.com/posts/313-strong-consistency-models ) 

It does not offer "Linearizable" or "Strong Serializable".

For fun, to illustrate the Jepsen framework, we tested Zookeeper against a higher level of consistency, namely "Linearizable", expecting it to fail that particular level of consistency.

The lessons learned illustrate how the Jepsen framework can be used to test distributed systems.
