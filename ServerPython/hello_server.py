import kimchi_pb2
import kimchi_pb2_grpc
from concurrent import futures
import grpc
import asyncio
import logging
from time import sleep
from random import randrange
# class GreeterServicer(kimchi_pb2_grpc.GreeterServicer):
#     def SayHello(self, request, context):
#         print(f"Received: {request.name}")
#         reply = kimchi_pb2.HelloReply()
#         reply.message = f"Hello, {request.name}"
#         print(f"About to reply with {reply.message}")
#         return reply

# def serve():
#     server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
#     greeter_servicer = GreeterServicer()
#     kimchi_pb2_grpc.add_GreeterServicer_to_server(greeter_servicer, server)
#     server.add_insecure_port("0.0.0.0:50051")
#     server.start()
#     print("Server started")
#     server.wait_for_termination()

class Greeter(kimchi_pb2_grpc.GreeterServicer):
    async def SayHello(
        self, request: kimchi_pb2.HelloRequest, context: grpc.aio.ServicerContext
    ) -> kimchi_pb2.HelloReply:
        logging.info("Serving sayHello request %s", request)
        # for i in range(10):
        i = 0
        while True:
            i += 1
            sleep(0.1)
            yield kimchi_pb2.HelloReply(message=f"Hello number {i}, {request.name}!")

class KimchiAppServicer(kimchi_pb2_grpc.KimchiAppServicer):
    async def GetPose(
        self, request: kimchi_pb2.Empty, context: grpc.aio.ServicerContext
    ) -> kimchi_pb2.Pose:
        logging.info("Serving GetPose request %s", request)
        while True:
            yield kimchi_pb2.Pose(x=randrange(10), y = randrange(10), theta = randrange(10))


async def serve() -> None:
    server = grpc.aio.server()
    # kimchi_pb2_grpc.add_GreeterServicer_to_server(Greeter(), server)
    kimchi_pb2_grpc.add_KimchiAppServicer_to_server(KimchiAppServicer(), server)
    listen_addr = "0.0.0.0:50051"
    server.add_insecure_port(listen_addr)
    logging.info("Starting server on %s", listen_addr)
    await server.start()
    await server.wait_for_termination()

if __name__ == "__main__":
    # serve()
    logging.basicConfig(level=logging.INFO)
    asyncio.run(serve())
