import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }

import cats.effect._
import cats.effect.syntax.all._
import cats.effect.ExitCase._
import cats.implicits._
import java.net.{ ServerSocket, Socket }

class Server {

  private def echoProtocol[F[_]: Sync](clientSocket: Socket): F[Unit] = {

    def loop(reader: BufferedReader, writer: BufferedWriter): F[Unit] =
      for {
        line <- Sync[F].delay(reader.readLine())
        _ <- line match {
              case "" => Sync[F].unit
              case _ =>
                Sync[F].delay {
                  writer.write(line)
                  writer.newLine()
                  writer.flush()
                } >> loop(reader, writer)
            }
      } yield ()

    def reader(clientSocket: Socket): Resource[F, BufferedReader] =
      Resource.make {
        Sync[F].delay(new BufferedReader(new InputStreamReader(clientSocket.getInputStream)))
      } { reader =>
        Sync[F].delay(reader.close()).handleErrorWith(_ => Sync[F].unit)
      }

    def writer(clientSocket: Socket): Resource[F, BufferedWriter] =
      Resource.make {
        Sync[F].delay(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream)))
      } { writer =>
        Sync[F].delay(writer.close()).handleErrorWith(_ => Sync[F].unit)
      }

    def readerWriter(clientSocket: Socket): Resource[F, (BufferedReader, BufferedWriter)] =
      for {
        input  <- reader(clientSocket)
        output <- writer(clientSocket)
      } yield (input, output)

    readerWriter(clientSocket).use {
      case (reader, writer) =>
        loop(reader, writer)
    }
  }

  def serve[F[_]: Concurrent](serverSocket: ServerSocket): F[Unit] = {

    def close(socket: Socket): F[Unit] = Sync[F].delay(socket.close()).handleErrorWith(_ => Sync[F].unit)

    for {
      _ <- Sync[F]
            .delay(serverSocket.accept())
            .bracketCase { socket =>
              echoProtocol(socket).guarantee(close(socket)).start
            } { (socket, exit) =>
              exit match {
                case ExitCase.Completed           => Sync[F].unit
                case Error(_) | ExitCase.Canceled => close(socket)
              }
            }
      _ <- serve(serverSocket)
    } yield ()
  }

}
