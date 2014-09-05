Zend Studio Workspace Refresher
==========================

Workspace refresh provider for Zend Studio using the File System API introduced in Java 7. Plugin provides native hooks refreshing mechanism for Linux (Eclipse for Windows has own implementaion for native hooks). This plugin will keep the workspace synchronized with the underlying filesystem. 

NOTE: Plugin can be used with any Eclipse package (3.8+). Not only with Zend Studio;-)

# Installation
Update site:
```
http://wywrzal.vipserv.org/swr_just_temporary_us/
```
IMPORTANT: Plugin will be working only with Java 1.7+

# Configuration
Go to *Window -> Preferences -> General -> Workspace* and select option *Refresh using native hooks or pooling*.

![screenshot](https://raw.githubusercontent.com/zendtech/studio-workspace-refresher/master/com.zend.studio.workspace.refresher.parent/resources/native_hooks_preference.png)

# Troubleshooting

Under Linux (inotify interface) default maximum inotify watches per user (directories to track) is usualy quite low (e.g. 8192). To check actual value for your system use:
```
cat /proc/sys/fs/inotify/max_user_watches
```
To change this value permanently add the following line in */etc/sysctl.conf*:
```
fs.inotify.max_user_watches=65536
```
Every user should adjust this value to environment and needs. More info about maximum value for *max_user_watches* can be found [here](http://askubuntu.com/questions/154255/how-can-i-tell-if-i-am-out-of-inotify-watches).

# License

Zend Studio Workspace Refresher is distributed under the [EPL](https://www.eclipse.org/legal/epl-v10.html).
