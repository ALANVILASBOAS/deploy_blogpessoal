package br.org.generation.blogpessoal.service;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.org.generation.blogpessoal.model.Usuario;
import br.org.generation.blogpessoal.model.UsuarioLogin;
import br.org.generation.blogpessoal.repository.UsuarioRepository;

/**
 *  A Classe UsuarioService implementa as regras de negócio do Usuario.
 *  
 *  Regras de negócio são as particularidades das funcionalidades a serem 
 *  implementadas no objeto, tais como:
 *  
 *  1) O Usuário não pode estar duplicado no Banco de dados
 *  2) O Usuario deve ser maior de 18 anos
 *  
 *  Observe que toda a implementação dos metodos Cadastrar, Atualizar e 
 *  Logar estão implmentadas na classe de serviço, enquanto a Classe
 *  Controller se limitará a checar se deu certo ou errado a requisição.
 */

 /**
 * A annotation @Service indica que esta é uma Classe de Serviço, ou seja,
 * implementa regras de negócio da aplicação
 */

@Service
public class UsuarioService {


	@Autowired
	private UsuarioRepository usuarioRepository;
	
	/**
	 * Cria o método findAll() na Classe de Serviço.
	 */
	public List<Usuario> listarUsuarios(){

		return usuarioRepository.findAll();

	}
	
	public Optional<Usuario> cadastrarUsuario(Usuario usuario) {

		/**
		 *  Checa se o usuário já existe no Banco de Dados. 
		 *  Se não existir retorna quer o usuario já existe
		 *  
		 *  isPresent() -> Se um valor estiver presente retorna true, caso contrário
		 *  retorna false.
		 * 
		 *  return Optional.empty() -> Retorna um optional vazio que será tratado na 
		 *  Classe Controladora
		 */

		if (usuarioRepository.findByUsuario(usuario.getUsuario()).isPresent())
			return Optional.empty();

		/**
		 *  Instancia um objeto da Classe BCryptPasswordEncoder para criptografar
		 *  a senha
		 */
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		/**
		 * Cria a variável senhaEncoder que receberá a senha criptografada.
		 * Para recuperar a senha do objeto usuario, foi utilizado o método Get.
		 */
		String senhaEncoder = encoder.encode(usuario.getSenha());
		
		/**
		 * Atualiza a senha do objeto usuário (enviado via Postman), trocando a senha
		 * não criptografada pela senha criptografada através do método Set
		 */
		usuario.setSenha(senhaEncoder);

		/**
		 * Retorna para a Classe UsuarioController o objeto Salvo no Banco de Dados
		 * A Classe controladora checará se deu tudo certo nesta operação
		 * 
		 * Optional.of -> Retorna um Optional com o valor fornecido, mas o valor não 
		 * pode ser nulo. Como nosso método possui um Optional na sua assinatura, 
		 * o retorno também deve ser um Optional.
		 */
		return Optional.of(usuarioRepository.save(usuario));

	}

	/**
	 *  A principal função do método autenticarUsuario, que é executado no endpoint logar,
	 *  é gerar o token do usuário codificado em Base64. O login prorpiamente dito é executado
	 *  pela BasicSecurityConfig em conjunto com as classes UserDetailsService e Userdetails
	 */
	public Optional<UsuarioLogin> autenticarUsuario(Optional<UsuarioLogin> usuarioLogin) {

		/**
		 *  Instancia um objeto da Classe BCryptPasswordEncoder para criptografar
		 *  a senha
		 */
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		
		/**
		 * Cria um objeto usuario para receber o resultado do metodo findByUsuario().
		 * 
		 * Observe que o método autenticarUsuario recebe como parâmetro um objeto da
		 * Classe UsuarioLogin.
		 * 
		 * get() -> Se um valor estiver presente no objeto ele retorna o valor, caso contrário,
		 * lança uma Exception NoSuchElementException. Então para usar get é preciso ter certeza 
		 * de que o Optional não está vazio.
		 * 
		 */
		Optional<Usuario> usuario = usuarioRepository.findByUsuario(usuarioLogin.get().getUsuario());

		/**
		 * Checa se o usuario existe
		 */
		if (usuario.isPresent()) {
			
			/**
			 *  Checa se a senha enviada pelo Postman, depois de criptografada, é igual a senha
			 *  gravada no Banco de Dados.
			 * 
			 *  matches -> Verifca se a senha codificada obtida do banco de dados corresponde à 
			 *  senha enviada depois que ela também for codificada. Retorna verdadeiro se as 
			 *  senhas coincidem e falso se não coincidem.  
			 * 
			 */
			if (encoder.matches(usuarioLogin.get().getSenha(), usuario.get().getSenha())) {

				/**
				 * As próximas 3 linhas tem a função de gerar o Token do tipo Basic.
				 * 
				 * A primeira linha, monta uma String via concatenação de carcateres
				 * que será codificada (Não criptografada) em Base64. 
				 * 
				 * Essa String tem o formato padrão: <username>:<password> que não pode ser
				 * alterado
				 */
				String auth = usuarioLogin.get().getUsuario() + ":" + usuarioLogin.get().getSenha();
				
				/**
				 * Na segunda linha, através da dependência commons-codec, faremos a codificação
				 * em Base 64 da String. 
				 * 
				 * Observe que a variável encodeAuth é um vetor do tipo Byte para receber o 
				 * resultado da codificação, porquê durante o processo é necessário trabalhar
				 * diretamente com os bytes da String
				 * 
				 * Base64.encodeBase64 -> aplica o algoritmo de codificação para Base64
				 * 
				 * Charset.forName("US-ASCII") -> Retorna o codigo ASCII de cada caractere da String.
				 */
				byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
				
				/**
				 * Na terceira linha, acrescenta a palavra Basic acompanhada de uma espaço em branco,
				 * além de converter o vetor de Bytes em String e concatenar tudo em uma única String
				 * e guarda esta nformação na variável authHeader
				 * 
				 * O espaço depois da palavra Basic é obrigatório. Caso não seja inserrido, o Token não
				 * será reconhecido.
				 */
				String authHeader = "Basic " + new String(encodedAuth);

				/**
				 * Atualiza o objeto usuarioLogin com os dados recuperados do Banco de Dados e o Token
				 */
				usuarioLogin.get().setId(usuario.get().getId());
				usuarioLogin.get().setNome(usuario.get().getNome());
				usuarioLogin.get().setSenha(usuario.get().getSenha());
				usuarioLogin.get().setToken(authHeader);

				/**
				 * Retorna o objeto usarioLogin atualizado para a classe Classe UsuarioController.
				 * A Classe controladora checará se deu tudo certo nesta operação
				 */
				return usuarioLogin;

			}
		}
		
		/**
		 *  return Optional.empty() -> Retorna um optional vazio que será tratado na 
		 *  Classe Controladora
		 */
		return Optional.empty();
	}

}