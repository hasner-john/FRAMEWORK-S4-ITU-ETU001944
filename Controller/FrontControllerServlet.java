package Controller;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class FrontControllerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws ServletException, IOException {

        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {

        processRequest(req, resp);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String url = req.getRequestURI();

        output(url, req, resp);
    }

    private void output(String url, HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {

        // recherche du mapping correspondant

        resp.getWriter().println("URL recue : " + url);
    }
}