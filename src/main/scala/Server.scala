import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }
import cats.effect.syntax.all._
import cats.effect.ExitCase._
import java.net.{ ServerSocket, Socket }
import cats.effect._
import cats.implicits._
import cats.effect.concurrent.MVar

class Server {

  private def echoProtocol[F[_]: Sync](clientSocket: Socket, stopFlag: MVar[F, Unit]): F[Unit] = {

    def loop(reader: BufferedReader, writer: BufferedWriter, stopFlag: MVar[F, Unit]): F[Unit] =
      for {
        line <- Sync[F].delay(reader.readLine()).attempt
        _ <- line match {
              case Left(e) =>
                for {
                  isEmpty <- stopFlag.isEmpty
                  _       <- if (!isEmpty) Sync[F].unit else Sync[F].raiseError[Unit](e)
                } yield ()
              case Right(value) =>
                value match {
                  case "STOP" => stopFlag.put(())
                  case ""     => Sync[F].unit
                  case _ =>
                    Sync[F].delay {
                      writer.write(value)
                      writer.newLine()
                      writer.flush()
                    } >> loop(reader, writer, stopFlag)
                }
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
      case (reader, writer) => loop(reader, writer, stopFlag)
    }
  }

  private def serve[F[_]: Concurrent](serverSocket: ServerSocket, stopFlag: MVar[F, Unit]): F[Unit] = {

    def close(socket: Socket): F[Unit] = Sync[F].delay(socket.close()).handleErrorWith(_ => Sync[F].unit)

    for {
      socket <- Sync[F]
                 .delay(serverSocket.accept())
                 .bracketCase { socket =>
                   echoProtocol(socket, stopFlag).guarantee(close(socket)).start >> Sync[F].pure(socket)
                 } { (socket, exit) =>
                   exit match {
                     case ExitCase.Completed           => Sync[F].unit
                     case Error(_) | ExitCase.Canceled => close(socket)
                   }
                 }
      _ <- (stopFlag.read >> close(socket)).start
      _ <- serve(serverSocket, stopFlag)
    } yield ()
  }

  def server[F[_]: Concurrent](serverSocket: ServerSocket): F[ExitCode] =
    for {
      stopFlag    <- MVar[F].empty[Unit]
      serverFiber <- serve(serverSocket, stopFlag).start
      _           <- stopFlag.read
      _           <- serverFiber.cancel.start
    } yield ExitCode.Success
}
