package realTcpServer

import java.net.InetSocketAddress

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, ContextShift, Sync}
import fs2.Stream
import fs2.io.tcp.SocketGroup
import io.chrisdavenport.log4cats.Logger
import realTcpServer.connections.ConnectionsCache

import scala.collection.immutable.HashMap

class Server {

  def start[F[_]: Concurrent: ContextShift: Logger](socketGroup: SocketGroup, port: Int) =
    Stream.eval_(Logger[F].info(s"Starting tcp server at port $port")) ++
      Stream.eval(ConnectionsCache[F]).flatMap { connections =>
        socketGroup.server[F](new InetSocketAddress(port)).map { connectionSocketResouce =>
          Stream.resource(connectionSocketResouce).flatMap {
            connectionSocket => Stream.bracket()
          }
        }
      }

}
