# pyAttacker
Some DoS script write by python.

### HOW TO USE

#### Requirements

- python > 3.6+

#### UDP, ICMP AND SYN Attack

1. Execute:

   ```shell
   sudo python3 DoS-attack.py <host to attack>
   ```

>  After the attack, you will see a file that records the time each attack started :)

#### HTTP Connection Attack

1. Execute:

   ```shell
   sudo python3 tcp-attacker.py <host to attack> -s <sockets count> -p <port> 
   ```

2. Paramets Introduction

   - `-s`: Number of socket connections created in one attack, default 150
   - `-p`: The port being attacked, default 80



**Have Fun :)**

