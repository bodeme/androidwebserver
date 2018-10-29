## lightweight Web Server (lWS) for Android

<a href="https://play.google.com/store/apps/details?id=net.basov.lws.r"><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="80" alt="Available on Google Play" /></a>
<a href="https://travis-ci.org/mvbasov/lWS"><img src="https://travis-ci.org/mvbasov/lWS.svg?branch=master" alt="Tavis CI Build Status" /></a>
<a href="https://f-droid.org/en/packages/net.basov.lws.fdroid/"><img src="https://f-droid.org/badge/get-it-on.png" height="80" alt="Available on F-Droid" /></a>

<table>
  <tr>
    <td width="20%"><img style="max-width:25%;height:auto" src="ic_launcher-web.png" alt="lWS" /><h3 align="center">lWS</h3></td>
    <td width="80%">
      <h3>It is ...</h3>
      <ul>
  <li>... Web Server for static content.</li>
  <li>... lightweight. APK size less then 100 Kb.</li>
  <li>... as simple as possible. Only essential features implemented.</li>
  <li>... open. <a href="https://github.com/mvbasov/lWS">Source code</a> released under GPL-3.0.</li>
  <li>... personal solution. It is not optimized/tested for many parallel connections and large file transfer.</li>
  <li>... network state responsive. Require WiFi connected or tethering enabled. Service stop automatically if network disconnected.</li>
      </ul>
    </td>
  </tr>
</table>

### Derivate from
This project based on [another open source project](https://github.com/bodeme/androidwebserver)
Unfortunately, original project didn't maintained for 3 years.

### What is configurable
* Document root. Path may be entered as text or optional elected using OI File Manager.
* Port. May be from 1024 to 65535. Default is 8080
In attempt to set wrong value parameter automatically set to default.

### Document root
Document root by default set to application private directory. Example index file automatically created. It is safe configuration. You can place your pages in this directory. But be carefully! If you use Android 5.0 or above and deinstall the application this directory and it's content will be removed.

### "Open in browser" and "Send URL"
After server starts you can press "Open in browser" button for check.
You can send working server URL to another device by Bluetooth, Android Beam, E-Mail and other way available on your device.

### On screen log
The application has no permanent logging. I treat this as redundant functionality. I doing my best to make notification actual all time. On screen log actual only then application visible. Log screen may be cleared after returning from background.

### Security warning
You can change document root to any readable point of file system, but you need to understand what are you doing.
<b>Be careful: you could (suddenly?) create the configuration so way, than anyone on the same WiFi network could access to the data on your device either you don't like it.</b>
All files from document root and below available for reading without any restrictions to anyone who connected to network and known URL of the server.

### License
lWS is licensed under the [GPLv3 License](LICENSE) because [original project](https://github.com/bodeme/androidwebserver)
Directory listing sort based on [this project](https://github.com/wmentzel/table-sort) licensed under GPL-3.0

### Artwork
* File listing icons from [Feather project](https://feathericons.com/) released under MIT license.
* Application icon designed specially for this application.

