package realTcpServer.connections

final case class Connected[F[_]](id: String, name: String)