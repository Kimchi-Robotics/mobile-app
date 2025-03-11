# mobile-app

## Prerequisites

- Install [Android studio](https://developer.android.com/studio/install?gad_source=1&gclid=Cj0KCQjw9Km3BhDjARIsAGUb4nzG1BMTh53o3cAZe1YG218ex1uAQWTuxvHMoOcKjaFe3Pq_CJNxUDwaAlZ9EALw_wcB&gclsrc=aw.ds&hl=es-419)

- Enable USB debugging on your device: [Instructions](https://developer.android.com/studio/debug/dev-options)

- Intall net-tools `sudo apt install net-tools`. This is used to get the ip of the computer in the local network.

## Run

### Get ip

- Open a terminal and run `ifconfig`.
- Your ip is the next one after 127.0.0.1. e.j:

```
br-54a4fb7cc096: flags=4099<UP,BROADCAST,MULTICAST>  mtu 1500
        inet 172.21.123.1  netmask 255.255.0.0  broadcast 172.21.255.255
        ether 02:42:4b:f7:18:c1  txqueuelen 0  (Ethernet)
        RX packets 0  bytes 0 (0.0 B)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 0  bytes 0 (0.0 B)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

docker0: flags=4099<UP,BROADCAST,MULTICAST>  mtu 1500
        inet 172.17.2.17  netmask 255.255.0.0  broadcast 172.17.255.255
        ether 02:42:c6:c5:d8:82  txqueuelen 0  (Ethernet)
        RX packets 0  bytes 0 (0.0 B)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 0  bytes 0 (0.0 B)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

enp2s0: flags=4099<UP,BROADCAST,MULTICAST>  mtu 1500
        ether 7c:8a:e1:ad:cd:af  txqueuelen 1000  (Ethernet)
        RX packets 0  bytes 0 (0.0 B)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 0  bytes 0 (0.0 B)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536
        inet 127.0.0.1  netmask 255.0.0.0
        inet6 ::1  prefixlen 128  scopeid 0x10<host>
        loop  txqueuelen 1000  (Local Loopback)
        RX packets 213548  bytes 396617826 (396.6 MB)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 213548  bytes 396617826 (396.6 MB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

wlo1: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 192.171.10.45  netmask 255.255.255.0  broadcast 192.171.10.255
        inet6 fe80::764c:f1:dd51:1a4a  prefixlen 64  scopeid 0x20<link>
        ether a8:93:4a:6f:68:8b  txqueuelen 1000  (Ethernet)
        RX packets 1951171  bytes 2254667888 (2.2 GB)
        RX errors 0  dropped 4  overruns 0  frame 0
        TX packets 799151  bytes 201216204 (201.2 MB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0
```

In this case, the IP will be `192.171.10.45`.


### Run Mobile App

- Open Android Studio
- Build protocol buffers
- Set the ip 
- Run the App

### Run the python server for testing

- In a new terminal go to the server dir: `cd ServerPython`
- Run:

```
./1_prepare_venv.sh
./2_gen_grpc_and_protobuf.sh
source ./venv/bin/activate
```

- `python3 hello_server.py`
