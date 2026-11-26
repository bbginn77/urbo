# URBO

The national portal of the United Republics of Brightport and Occasue (URBO), a fictional nation on the planet Nyreth,
as an Android app for phones, tablets and Android TV / Google TV (like the onn stick).

Everything runs offline from inside the app:

- **Government, Territories, Cities & transport, History** and the **national language** pages
- **Browse by level**: URBO, the two republics, every state and territory, and their shires, comitatus and rejions
- **Map**: tap a state to open its page
- **URBO Weather**, **URBO Traffic & Rail** and **BNN** (Brightport News Network), the same simulated channels as NyxVision channels 2, 10 and 11
- **EN / BP**: switch the whole app to Brightportish, in the national script

## Install

GitHub builds the app on every push. On the onn stick or tablet, open **Downloader** and enter:

```
https://github.com/BBGINN77/urbo/releases/latest/download/urbo.apk
```

## Remote

- **D-pad** moves the yellow focus ring to the nearest link or button; **OK** opens it
- **BACK** goes to the previous page, and leaves the app from the home page

## Updating the site inside the app

The web pages live in `app/src/main/assets/www`. To refresh them from the published site files:

```
python3 tools/sync_site.py /path/to/site
```
