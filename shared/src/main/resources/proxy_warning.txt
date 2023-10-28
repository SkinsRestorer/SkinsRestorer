SkinsRestorer is running in "Proxy Mode"

If you are NOT using a proxy (BungeeCord, Waterfall or Velocity) in your network, set spigot.yml -> bungeecord: false

Using a proxy? --> You MUST use proxy installation! (or use override files)

What is "Proxy Mode"?
The proxy will handle SkinsRestorer's commands, API, permission, database and config in a "centralized" manner.

How to install on proxy?
Please read: https://skinsrestorer.net/docs/installation#platform-installation

Your proxy will now tell all your backend servers what to do!
You may now configure SkinsRestorer on your proxy (<proxy>/plugins/SkinsRestorer)

--- Override Files ---

Backend API:
You can enable the backend storage by creating a file called enableSkinStorageAPI.txt in SkinsRestorer folder (backend server -> ./plugins/SkinsRestorer/enableSkinStorageAPI.txt)
This is useful when using plugins like BedWars, Dynmap, etc. that need to get the skin through API from the backend server.
Keep in mind that connecting MySQL to the same database as the proxy is required for this to work!

ProxyMode:
Warning: Disabling ProxyMode will force the backend to handle everything including applying skin on join. This is not recommended as the proxy has way better control over the player connection.
How to turn off proxy mode: create a file called disableProxyMode.txt in SkinsRestorer folder (backend server -> ./plugins/SkinsRestorer/disableProxyMode.txt)
This will force disable proxy mode on next restart.
