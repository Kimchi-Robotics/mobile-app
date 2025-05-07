# mobile-app

## Prerequisites

- Install [Android studio](https://developer.android.com/studio/install?gad_source=1&gclid=Cj0KCQjw9Km3BhDjARIsAGUb4nzG1BMTh53o3cAZe1YG218ex1uAQWTuxvHMoOcKjaFe3Pq_CJNxUDwaAlZ9EALw_wcB&gclsrc=aw.ds&hl=es-419)

- Enable USB debugging on your device: [Instructions](https://developer.android.com/studio/debug/dev-options)

## Run

### Run Mobile App

- Open Android Studio
- Build protocol buffers
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
