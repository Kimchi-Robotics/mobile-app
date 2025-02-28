import hello_world_pb2
import hello_world_pb2_grpc
from concurrent import futures
import grpc

class GreeterServicer(hello_world_pb2_grpc.GreeterServicer):
    def SayHello(self, request, context):
        print(f"Received: {request.name}")
        reply = hello_world_pb2.HelloReply()
        reply.message = f"Hello, {request.name}"
        print(f"About to reply with {reply.message}")
        return reply

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
    greeter_servicer = GreeterServicer()
    hello_world_pb2_grpc.add_GreeterServicer_to_server(greeter_servicer, server)
    server.add_insecure_port("0.0.0.0:50051")
    server.start()
    print("Server started")
    server.wait_for_termination()

if __name__ == "__main__":
    serve()
