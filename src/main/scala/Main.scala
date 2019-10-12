import cats.implicits._
import java.net.ServerSocket
import cats.effect.{ ExitCode, IO, IOApp, Sync }

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    def close[F[_]: Sync](socket: ServerSocket): F[Unit] =
      Sync[F].delay(socket.close()).handleErrorWith(_ => Sync[F].unit)

    val server: Server = new Server

    IO(new ServerSocket(args.headOption.map(_.toInt).getOrElse(9999))).bracket { serverSocket =>
      server.server[IO](serverSocket) >> IO.pure(ExitCode.Success)
    } { serverSocket =>
      close[IO](serverSocket) >> IO(println("Server finished"))
    }
  }
}
