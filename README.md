# MixDos
## Minecraft server stress tool.
Proxy finder + checker is bundled <br>
Protocol version auto-detect <br>
Infinite bots count <br>
Customizable bot prefix <br>
Commands on join <br>
**[Downloads](https://github.com/MeexReay/mixdos/releases/tag/1.5)**

![image](https://github.com/MeexReay/mixdos/assets/127148610/d7ee6657-401d-4298-a5bd-0d8ba86a33d0)

## Parameters
|      **Name**     |                    **Description**                   |            **Usage**           |  **Default**  | **Required** |
|:-----------------:|:----------------------------------------------------:|:------------------------------:|:-------------:|:------------:|
| IP                | Server ip address                                    | --ip IP[:PORT]                 | port is 25565 | Yes          |
| Protocol          | Server protocol version                              | --protocol \<VERSION\>           | autodetect    | No           |
| Count             | Number of bots to connect                            | --count \<NUMBER\>               | infinite (-1) | No           |
| Delay             | Milliseconds between bot connections                 | --delay \<MILLISECONDS\>         | 500           | No           |
| Proxies           | File with SOCKS5 proxy, on each line IP:PORT         | --proxy \<FILEPATH\>             | parses proxy  | No           |
| Parse time        | Seconds to parse proxies                             | --parse-time \<SECONDS\>         | 40            | No           |
| Prefix            | Bot nickname prefix                                  | --prefix \<STRING\>              | random chars  | No           |
| Commands          | Entering commands after logging into the server      | --cmds "/\<CMD1>" "/\<CMD2\>" ... | disabled      | No           |
| Commands delay    | Specific milliseconds before each command is entered | --cmds-delay \<VALUE\>           | 1000          | No           |
| Random cmds delay | Random milliseconds before each command is entered   | --cmds-delay \<MIN\> \<MAX\>       | 1000 - 1000   | No           |
| Debug mode        | Enables debug mode                                   | --debug                        | disabled      | No           |

# ONLY FOR TESTING PROTECTION PLUGINS
